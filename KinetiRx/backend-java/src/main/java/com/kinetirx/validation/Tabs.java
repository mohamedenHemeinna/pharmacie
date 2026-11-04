package com.kinetirx.validation;

import java.util.List;

/** Every tab/permission identifier. Same list and order as models.AllTabTypes in Go. */
public final class Tabs {

    public static final List<String> ALL = List.of(
            "dashboard", "daily-calc", "daily-sales", "pos", "due-khata",
            "medicine-orders", "inventory", "inward", "inward-ocr", "opd",
            "patients", "expenses", "business-dev", "employee-mgmt",
            "invoice-settings", "system-reset");

    private Tabs() {}
}
