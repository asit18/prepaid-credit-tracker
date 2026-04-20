package com.yourcompany.credittracker.controller;

import com.yourcompany.credittracker.dto.*;
import com.yourcompany.credittracker.model.Customer;
import com.yourcompany.credittracker.model.TransactionType;
import com.yourcompany.credittracker.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.auth.google-enabled:false}")
    private boolean googleEnabled;

    @GetMapping("/login")
    String login(Model model) {
        model.addAttribute("googleEnabled", googleEnabled);
        return "login";
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
                   @RequestParam(required = false) TransactionType type) {
        LocalDate effectiveFrom = from == null ? LocalDate.now() : from;
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        model.addAttribute("customers", customerService.allActive());
        model.addAttribute("products", productService.all());
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("from", effectiveFrom);
        model.addAttribute("to", effectiveTo);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("selectedType", type);
        model.addAttribute("summary", reportService.summary(effectiveFrom, effectiveTo, customerId, productId, type));
        model.addAttribute("rows", reportService.rows(effectiveFrom, effectiveTo, customerId, productId, type));
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
        return "admins";
    }

    @PostMapping("/admins")
    String addAdmin(@RequestParam String email, @RequestParam(required = false) String displayName, RedirectAttributes redirectAttributes) {
        adminUserService.addAdmin(email, displayName);
        redirectAttributes.addFlashAttribute("toast", "Admin added");
        return "redirect:/admins";
    }

    @PostMapping("/admins/{id}/active")
    String adminActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes redirectAttributes) {
        adminUserService.setActive(id, active);
        redirectAttributes.addFlashAttribute("toast", "Admin updated");
        return "redirect:/admins";
    }

    @GetMapping("/settings")
    String settings() {
        return "settings";
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
