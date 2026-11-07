package com.kuet.ums.web;

import com.kuet.ums.dto.StudentRequest;
import com.kuet.ums.dto.StudentResponse;
import com.kuet.ums.exception.BadRequestException;
import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.exception.ForbiddenException;
import com.kuet.ums.exception.NotFoundException;
import com.kuet.ums.service.AccountAccess;
import com.kuet.ums.service.StudentService;
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
@RequestMapping("/students")
public class StudentPageController {

    private final StudentService studentService;

    public StudentPageController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public String list(Model model) {
        prepare(model, new StudentRequest(), new StudentRequest(), null);
        return "students";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            StudentResponse existing = studentService.get(id);
            AccountAccess.requireOwnerOrAdmin(existing.username());
            prepare(model, new StudentRequest(), toForm(existing), id);
            return "students";
        } catch (NotFoundException | ForbiddenException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/students";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("createForm") StudentRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            prepare(model, form, new StudentRequest(), null);
            return "students";
        }
        try {
            studentService.create(form);
        } catch (ConflictException | BadRequestException | ForbiddenException exception) {
            prepare(model, form, new StudentRequest(), null);
            model.addAttribute("formError", exception.getMessage());
            return "students";
        }
        redirect.addFlashAttribute("success", "Student created.");
        return "redirect:/students";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("editForm") StudentRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            prepare(model, new StudentRequest(), form, id);
            return "students";
        }
        try {
            studentService.update(id, form);
        } catch (NotFoundException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/students";
        } catch (ConflictException | BadRequestException | ForbiddenException exception) {
            prepare(model, new StudentRequest(), form, id);
            model.addAttribute("formError", exception.getMessage());
            return "students";
        }
        redirect.addFlashAttribute("success", "Student updated.");
        return "redirect:/students";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            studentService.delete(id);
            redirect.addFlashAttribute("success", "Student deleted.");
        } catch (NotFoundException | ConflictException | ForbiddenException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/students";
    }

    private void prepare(Model model, StudentRequest createForm, StudentRequest editForm, Long editingId) {
        model.addAttribute("students", studentService.list());
        model.addAttribute("createForm", createForm);
        model.addAttribute("editForm", editForm);
        model.addAttribute("editingId", editingId);
    }

    private StudentRequest toForm(StudentResponse existing) {
        StudentRequest form = new StudentRequest();
        form.setFullName(existing.fullName());
        form.setEmail(existing.email());
        form.setUsername(existing.username());
        form.setStudentId(existing.studentId());
        form.setDepartment(existing.department());
        form.setBatch(existing.batch());
        return form;
    }
}
