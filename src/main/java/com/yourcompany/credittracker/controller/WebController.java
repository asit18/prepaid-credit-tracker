package com.yourcompany.credittracker.controller;

import com.yourcompany.credittracker.dto.*;
import com.yourcompany.credittracker.model.AdminRole;
import com.yourcompany.credittracker.model.AdminUser;
import com.yourcompany.credittracker.model.CreditTransaction;
import com.yourcompany.credittracker.model.Customer;
import com.yourcompany.credittracker.model.TransactionType;
import com.yourcompany.credittracker.security.MfaSessionConstants;
import com.yourcompany.credittracker.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {
    private final CustomerService customerService;
    private final ProductService productService;
    private final CreditService creditService;
    private final ReportService reportService;
    private final AdminUserService adminUserService;
    private final AppDateTimeService appDateTimeService;
    private final AppSettingService appSettingService;
    private final CurrentAdminService currentAdminService;
    private final MfaService mfaService;
    private final PasswordResetService passwordResetService;

    @Value("${app.auth.google-enabled:false}")
    private boolean googleEnabled;

    @GetMapping("/login")
    String login(Model model) {
        model.addAttribute("googleEnabled", googleEnabled);
        return "login";
    }

    @GetMapping("/login/mfa")
    String loginMfa() {
        return "login-mfa";
    }

    @PostMapping("/login/mfa")
    String verifyLoginMfa(@RequestParam String code,
                          Authentication authentication,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            AdminUser admin = currentAdminService.currentAdmin(authentication);
            mfaService.verifyByUserId(admin.getId(), code);
            request.getSession().setAttribute(MfaSessionConstants.MFA_VERIFIED, true);
            return "redirect:/";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/login/mfa";
        }
    }

    @GetMapping("/access-denied")
    String accessDenied() {
        return "access-denied";
    }

    @GetMapping("/")
    String dashboard() {
        return "dashboard";
    }

    @GetMapping("/customers/new")
    String newCustomer(Model model, @RequestParam(required = false) Long customerId) {
        model.addAttribute("customerFormTitle", "Add Customer");
        if (customerId != null) {
            model.addAttribute("customer", customerService.get(customerId));
            model.addAttribute("customerFormAction", "/customers/" + customerId);
            model.addAttribute("editingCustomer", true);
        } else {
            model.addAttribute("customerFormAction", "/customers");
            model.addAttribute("editingCustomer", false);
        }
        return "customer-form";
    }

    @PostMapping("/customers")
    String createCustomer(@RequestParam String name,
                          @RequestParam(required = false) String notes,
                          @RequestParam(required = false) String email,
                          @RequestParam String phone,
                          @RequestParam(required = false, name = "contactName") List<String> contactNames,
                          @RequestParam(required = false, name = "contactEmail") List<String> contactEmails,
                          @RequestParam(required = false, name = "contactPhone") List<String> contactPhones,
                          @RequestParam(required = false, name = "contactRelationship") List<String> relationships,
                          RedirectAttributes redirectAttributes) {
        List<ContactRequest> contacts = contacts(contactNames, contactEmails, contactPhones, relationships);
        try {
            Customer customer = customerService.create(new CustomerRequest(name, notes, email, phone, contacts));
            redirectAttributes.addFlashAttribute("toast", "Customer saved");
            return "redirect:/customers/" + customer.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/customers/new";
        }
    }

    @GetMapping("/customers/{id}/edit")
    String editCustomer(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.get(id));
        model.addAttribute("customerFormTitle", "Add Customer");
        model.addAttribute("customerFormAction", "/customers/" + id);
        model.addAttribute("editingCustomer", true);
        return "customer-form";
    }

    @PostMapping("/customers/{id}")
    String updateCustomer(@PathVariable Long id,
                          @RequestParam String name,
                          @RequestParam(required = false) String notes,
                          @RequestParam(required = false) String email,
                          @RequestParam String phone,
                          @RequestParam(required = false, name = "contactName") List<String> contactNames,
                          @RequestParam(required = false, name = "contactEmail") List<String> contactEmails,
                          @RequestParam(required = false, name = "contactPhone") List<String> contactPhones,
                          @RequestParam(required = false, name = "contactRelationship") List<String> relationships,
                          RedirectAttributes redirectAttributes) {
        try {
            customerService.update(id, new CustomerRequest(name, notes, email, phone,
                    contacts(contactNames, contactEmails, contactPhones, relationships)));
            redirectAttributes.addFlashAttribute("toast", "Customer updated");
            return "redirect:/customers/" + id;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/customers/" + id + "/edit";
        }
    }

    @PostMapping("/customers/{id}/delete")
    String deleteCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        customerService.deactivate(id);
        redirectAttributes.addFlashAttribute("toast", "Customer deactivated");
        return "redirect:/customers/manage";
    }

    @GetMapping("/customers/manage")
    String customerLookup(Model model,
                          @RequestParam(required = false) Long customerId,
                          @RequestParam(required = false) String search) {
        model.addAttribute("products", productService.active());
        if (customerId != null) {
            model.addAttribute("search", "");
            model.addAttribute("customers", List.of());
            model.addAttribute("customer", customerService.get(customerId));
            model.addAttribute("balances", creditService.balances(customerId));
            model.addAttribute("transactions", creditService.history(customerId, PageRequest.of(0, 50)).getContent());
            CreditTransaction lastTransaction = creditService.lastTransaction(customerId);
            model.addAttribute("lastVisitDate", lastTransaction == null ? null : lastTransaction.getTransactionDate());
        } else {
            model.addAttribute("search", search);
            model.addAttribute("customers", customerService.search(search));
        }
        return "customer-manage";
    }

    @GetMapping("/customers/{id}")
    String customerDetail(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.get(id));
        model.addAttribute("balances", creditService.balances(id));
        model.addAttribute("transactions", creditService.history(id, PageRequest.of(0, 25)).getContent());
        return "customer-detail";
    }

    @PostMapping("/customers/{id}/transactions")
    String addTransaction(@PathVariable Long id,
                          @Valid TransactionRequest request,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            creditService.addTransaction(id, request, CurrentUser.email(authentication));
            redirectAttributes.addFlashAttribute("toast", "Transaction saved");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/customers/manage?customerId=" + id;
    }

    @GetMapping("/products")
    String products(Model model) {
        model.addAttribute("products", productService.all());
        return "products";
    }

    @PostMapping("/products")
    String saveProduct(ProductRequest request,
                       @RequestParam BigDecimal pricePerUnit,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {
        productService.createWithInitialPrice(request, new PriceRequest(pricePerUnit, effectiveFrom), CurrentUser.email(authentication));
        redirectAttributes.addFlashAttribute("toast", "Product saved");
        return "redirect:/products";
    }

    @PostMapping("/products/{id}")
    String updateProduct(@PathVariable Long id, ProductRequest request, RedirectAttributes redirectAttributes) {
        productService.update(id, request);
        redirectAttributes.addFlashAttribute("toast", "Product updated");
        return "redirect:/products";
    }

    @PostMapping("/products/{id}/delete")
    String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.deactivate(id);
        redirectAttributes.addFlashAttribute("toast", "Product deactivated");
        return "redirect:/products";
    }

    @PostMapping("/products/{id}/prices")
    String addPrice(@PathVariable Long id, PriceRequest request, Authentication authentication, RedirectAttributes redirectAttributes) {
        productService.addPrice(id, request, CurrentUser.email(authentication));
        redirectAttributes.addFlashAttribute("toast", "Price updated");
        return "redirect:/products";
    }

    @GetMapping("/reports")
    String reports(Model model,
                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                   @RequestParam(required = false) Long customerId,
                   @RequestParam(required = false) Long productId,
                   @RequestParam(required = false) TransactionType type,
                   @RequestParam(defaultValue = "0") int page,
                   @RequestParam(defaultValue = "25") int size) {
        List<ReportRow> allRows = reportService.rows(from, to, customerId, productId, type);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int start = Math.min(safePage * safeSize, allRows.size());
        int end = Math.min(start + safeSize, allRows.size());
        Page<ReportRow> rowsPage = new PageImpl<>(allRows.subList(start, end), PageRequest.of(safePage, safeSize), allRows.size());
        model.addAttribute("customers", customerService.allActive());
        model.addAttribute("products", productService.all());
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("selectedType", type);
        model.addAttribute("summary", reportService.summary(from, to, customerId, productId, type));
        model.addAttribute("rows", rowsPage.getContent());
        model.addAttribute("rowsPage", rowsPage);
        return "reports";
    }

    @GetMapping("/reports.csv")
    void reportsCsv(HttpServletResponse response,
                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                    @RequestParam(required = false) Long customerId,
                    @RequestParam(required = false) Long productId,
                    @RequestParam(required = false) TransactionType type) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.csv");
        response.getWriter().println("Customer,Product,Date,Type,Units,Amount Paid,Price/Unit,Balance After,Created By");
        for (ReportRow row : reportService.rows(from, to, customerId, productId, type)) {
            response.getWriter().printf("\"%s\",\"%s\",%s,%s,%s,%s,%s,%s,\"%s\"%n",
                    row.customer(), row.product(), appDateTimeService.format(row.date()), row.type(), row.units(), row.amountPaid(),
                    row.pricePerUnit(), row.balanceAfter(), row.createdBy());
        }
    }

    @GetMapping("/admins")
    String admins(Model model) {
        model.addAttribute("admins", adminUserService.all());
        model.addAttribute("roles", AdminRole.values());
        return "admins";
    }

    @PostMapping("/admins")
    String addAdmin(@RequestParam String email, @RequestParam(required = false) String displayName,
                    @RequestParam(defaultValue = "EMPLOYEE") AdminRole role, RedirectAttributes redirectAttributes) {
        adminUserService.addAdmin(email, displayName, role);
        redirectAttributes.addFlashAttribute("toast", "Admin added");
        return "redirect:/admins";
    }

    @PostMapping("/admins/{id}/role")
    String adminRole(@PathVariable Long id, @RequestParam AdminRole role, RedirectAttributes redirectAttributes) {
        adminUserService.updateRole(id, role);
        redirectAttributes.addFlashAttribute("toast", "Admin role updated");
        return "redirect:/admins";
    }

    @PostMapping("/admins/{id}/active")
    String adminActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes redirectAttributes) {
        adminUserService.setActive(id, active);
        redirectAttributes.addFlashAttribute("toast", "Admin updated");
        return "redirect:/admins";
    }

    @PostMapping("/admins/{id}/password-reset")
    String passwordReset(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        passwordResetService.triggerReset(id, CurrentUser.email(authentication));
        redirectAttributes.addFlashAttribute("toast", "Password reset email triggered");
        return "redirect:/admins";
    }

    @GetMapping("/password-reset")
    String passwordResetForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("valid", passwordResetService.isValid(token));
        return "password-reset";
    }

    @PostMapping("/password-reset")
    String passwordResetSubmit(@RequestParam String token,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        try {
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match");
            }
            passwordResetService.resetPassword(token, password);
            redirectAttributes.addFlashAttribute("toast", "Password updated. Sign in with the new password.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/password-reset?token=" + token;
        }
    }

    @GetMapping("/settings")
    String settings() {
        return "settings";
    }

    @GetMapping("/settings/security")
    String securitySettings(Model model, Authentication authentication) {
        AdminUser admin = currentAdminService.currentAdmin(authentication);
        model.addAttribute("admin", admin);
        return "security-settings";
    }

    @PostMapping("/settings/security/mfa/setup")
    String setupMfa(Authentication authentication, RedirectAttributes redirectAttributes) {
        AdminUser admin = currentAdminService.currentAdmin(authentication);
        MfaSetupResponse setup = mfaService.setup(admin.getId());
        redirectAttributes.addFlashAttribute("mfaSecret", setup.secret());
        redirectAttributes.addFlashAttribute("mfaQrCode", setup.qrCodeBase64Png());
        redirectAttributes.addFlashAttribute("mfaOtpAuthUri", setup.otpauthUri());
        redirectAttributes.addFlashAttribute("toast", "Scan the QR code and confirm MFA");
        return "redirect:/settings/security";
    }

    @PostMapping("/settings/security/mfa/confirm")
    String confirmMfa(@RequestParam String code, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            AdminUser admin = currentAdminService.currentAdmin(authentication);
            MfaConfirmResponse response = mfaService.confirm(admin.getId(), code);
            redirectAttributes.addFlashAttribute("backupCodes", response.backupCodes());
            redirectAttributes.addFlashAttribute("toast", "MFA enabled");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/settings/security";
    }

    @PostMapping("/settings/security/mfa/disable")
    String disableMfa(@RequestParam String password,
                      @RequestParam String code,
                      Authentication authentication,
                      HttpServletRequest request,
                      RedirectAttributes redirectAttributes) {
        try {
            AdminUser admin = currentAdminService.currentAdmin(authentication);
            currentAdminService.verifyPassword(admin, password);
            mfaService.disable(admin.getId(), code);
            request.getSession().setAttribute(MfaSessionConstants.MFA_VERIFIED, true);
            redirectAttributes.addFlashAttribute("toast", "MFA disabled");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/settings/security";
    }

    @PostMapping("/settings/business-name")
    String updateBusinessName(@RequestParam String businessName, RedirectAttributes redirectAttributes) {
        try {
            appSettingService.updateBusinessName(businessName);
            redirectAttributes.addFlashAttribute("toast", "Settings updated");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/settings";
    }

    private List<ContactRequest> contacts(List<String> names, List<String> emails, List<String> phones, List<String> relationships) {
        List<ContactRequest> contacts = new ArrayList<>();
        if (names == null) {
            return contacts;
        }
        for (int i = 0; i < names.size(); i++) {
            contacts.add(new ContactRequest(names.get(i), value(emails, i), value(phones, i), value(relationships, i)));
        }
        return contacts;
    }

    private String value(List<String> values, int index) {
        return values == null || index >= values.size() ? null : values.get(index);
    }
}
