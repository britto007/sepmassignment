package com.kuet.ums.web;

import com.kuet.ums.dto.AdminRequest;
import com.kuet.ums.dto.AdminResponse;
import com.kuet.ums.exception.BadRequestException;
import com.kuet.ums.exception.ConflictException;
import com.kuet.ums.exception.NotFoundException;
import com.kuet.ums.service.AdminService;
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
@RequestMapping("/admins")
public class AdminPageController {

    private final AdminService adminService;

    public AdminPageController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String list(Model model) {
        prepare(model, new AdminRequest(), new AdminRequest(), null);
        return "admins";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            AdminResponse existing = adminService.get(id);
            prepare(model, new AdminRequest(), toForm(existing), id);
            return "admins";
        } catch (NotFoundException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admins";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("createForm") AdminRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            prepare(model, form, new AdminRequest(), null);
            return "admins";
        }
        try {
            adminService.create(form);
        } catch (ConflictException | BadRequestException exception) {
            prepare(model, form, new AdminRequest(), null);
            model.addAttribute("formError", exception.getMessage());
            return "admins";
        }
        redirect.addFlashAttribute("success", "Admin created.");
        return "redirect:/admins";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("editForm") AdminRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            prepare(model, new AdminRequest(), form, id);
            return "admins";
        }
        try {
            adminService.update(id, form);
        } catch (NotFoundException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admins";
        } catch (ConflictException | BadRequestException exception) {
            prepare(model, new AdminRequest(), form, id);
            model.addAttribute("formError", exception.getMessage());
            return "admins";
        }
        redirect.addFlashAttribute("success", "Admin updated.");
        return "redirect:/admins";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            adminService.delete(id);
            redirect.addFlashAttribute("success", "Admin deleted.");
        } catch (NotFoundException | ConflictException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admins";
    }

    private void prepare(Model model, AdminRequest createForm, AdminRequest editForm, Long editingId) {
        model.addAttribute("admins", adminService.list());
        model.addAttribute("createForm", createForm);
        model.addAttribute("editForm", editForm);
        model.addAttribute("editingId", editingId);
    }

    private AdminRequest toForm(AdminResponse existing) {
        AdminRequest form = new AdminRequest();
        form.setFullName(existing.fullName());
        form.setEmail(existing.email());
        form.setUsername(existing.username());
        return form;
    }
}
