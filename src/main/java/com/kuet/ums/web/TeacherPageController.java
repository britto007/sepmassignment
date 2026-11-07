package com.kuet.ums.web;

import com.kuet.ums.dto.TeacherRequest;
import com.kuet.ums.dto.TeacherResponse;
import com.kuet.ums.exception.BadRequestException;
import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.exception.ForbiddenException;
import com.kuet.ums.exception.NotFoundException;
import com.kuet.ums.service.AccountAccess;
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
@RequestMapping("/teachers")
public class TeacherPageController {

    private final TeacherService teacherService;

    public TeacherPageController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping
    public String list(Model model) {
        prepare(model, new TeacherRequest(), new TeacherRequest(), null);
        return "teachers";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            TeacherResponse existing = teacherService.get(id);
            AccountAccess.requireOwnerOrAdmin(existing.username());
            prepare(model, new TeacherRequest(), toForm(existing), id);
            return "teachers";
        } catch (NotFoundException | ForbiddenException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/teachers";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("createForm") TeacherRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            prepare(model, form, new TeacherRequest(), null);
            return "teachers";
        }
        try {
            teacherService.create(form);
        } catch (ConflictException | BadRequestException | ForbiddenException exception) {
            prepare(model, form, new TeacherRequest(), null);
            model.addAttribute("formError", exception.getMessage());
            return "teachers";
        }
        redirect.addFlashAttribute("success", "Teacher created.");
        return "redirect:/teachers";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("editForm") TeacherRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            prepare(model, new TeacherRequest(), form, id);
            return "teachers";
        }
        try {
            teacherService.update(id, form);
        } catch (NotFoundException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/teachers";
        } catch (ConflictException | BadRequestException | ForbiddenException exception) {
            prepare(model, new TeacherRequest(), form, id);
            model.addAttribute("formError", exception.getMessage());
            return "teachers";
        }
        redirect.addFlashAttribute("success", "Teacher updated.");
        return "redirect:/teachers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            teacherService.delete(id);
            redirect.addFlashAttribute("success", "Teacher deleted.");
        } catch (NotFoundException | ConflictException | ForbiddenException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/teachers";
    }

    private void prepare(Model model, TeacherRequest createForm, TeacherRequest editForm, Long editingId) {
        model.addAttribute("teachers", teacherService.list());
        model.addAttribute("createForm", createForm);
        model.addAttribute("editForm", editForm);
        model.addAttribute("editingId", editingId);
    }

    private TeacherRequest toForm(TeacherResponse existing) {
        TeacherRequest form = new TeacherRequest();
        form.setFullName(existing.fullName());
        form.setEmail(existing.email());
        form.setUsername(existing.username());
        form.setDepartment(existing.department());
        form.setDesignation(existing.designation());
        return form;
    }
}
