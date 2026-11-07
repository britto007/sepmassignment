package com.kuet.ums.web;

import com.kuet.ums.dto.CourseRequest;
import com.kuet.ums.dto.CourseResponse;
import com.kuet.ums.exception.BadRequestException;
import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.exception.ForbiddenException;
import com.kuet.ums.exception.NotFoundException;
import com.kuet.ums.service.CourseService;
import com.kuet.ums.service.CurrentUser;
import com.kuet.ums.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/courses")
public class CoursePageController {

    private final CourseService courseService;
    private final TeacherService teacherService;

    public CoursePageController(CourseService courseService, TeacherService teacherService) {
        this.courseService = courseService;
        this.teacherService = teacherService;
    }

    @GetMapping
    public String list(Model model) {
        prepare(model, new CourseRequest(), new CourseRequest(), null);
        return "courses";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            courseService.ensureCanEdit(id);
            prepare(model, new CourseRequest(), toForm(courseService.get(id)), id);
            return "courses";
        } catch (NotFoundException | ForbiddenException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/courses";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("createForm") CourseRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            prepare(model, form, new CourseRequest(), null);
            return "courses";
        }
        try {
            courseService.create(form);
        } catch (ConflictException | BadRequestException | ForbiddenException | NotFoundException exception) {
            prepare(model, form, new CourseRequest(), null);
            model.addAttribute("formError", exception.getMessage());
            return "courses";
        }
        redirect.addFlashAttribute("success", "Course created.");
        return "redirect:/courses";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("editForm") CourseRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            prepare(model, new CourseRequest(), form, id);
            return "courses";
        }
        try {
            courseService.update(id, form);
        } catch (NotFoundException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/courses";
        } catch (ConflictException | BadRequestException | ForbiddenException exception) {
            prepare(model, new CourseRequest(), form, id);
            model.addAttribute("formError", exception.getMessage());
            return "courses";
        }
        redirect.addFlashAttribute("success", "Course updated.");
        return "redirect:/courses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            courseService.delete(id);
            redirect.addFlashAttribute("success", "Course deleted.");
        } catch (NotFoundException | ForbiddenException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/courses";
    }

    @PostMapping("/{id}/enroll")
    public String enroll(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            courseService.enrollCurrentStudent(id);
            redirect.addFlashAttribute("success", "You are enrolled.");
        } catch (NotFoundException | ConflictException | ForbiddenException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/courses";
    }

    @PostMapping("/{id}/drop")
    public String drop(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            courseService.dropCurrentStudent(id);
            redirect.addFlashAttribute("success", "You dropped the course.");
        } catch (NotFoundException | ForbiddenException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/courses";
    }

    @PostMapping("/{id}/enrollments/{studentId}/remove")
    public String removeStudent(
            @PathVariable Long id,
            @PathVariable Long studentId,
            RedirectAttributes redirect) {
        try {
            courseService.removeStudent(id, studentId);
            redirect.addFlashAttribute("success", "Student removed from the course.");
        } catch (NotFoundException | ForbiddenException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/courses";
    }

    private void prepare(Model model, CourseRequest createForm, CourseRequest editForm, Long editingId) {
        model.addAttribute("courses", courseService.list());
        model.addAttribute("createForm", createForm);
        model.addAttribute("editForm", editForm);
        model.addAttribute("editingId", editingId);
        if (CurrentUser.isAdmin()) {
            model.addAttribute("teachers", teacherService.list());
        }
    }

    private CourseRequest toForm(CourseResponse existing) {
        CourseRequest form = new CourseRequest();
        form.setCode(existing.code());
        form.setTitle(existing.title());
        form.setDepartment(existing.department());
        form.setCreditHours(existing.creditHours());
        form.setTeacherId(existing.teacherId());
        return form;
    }
}
