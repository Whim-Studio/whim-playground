package com.whim.oggalaxy.model;

import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.Views;

import java.io.Serializable;

/** An expedition outcome report. Implements {@link Views.ExpeditionReportView}. */
public final class ExpeditionReport implements Views.ExpeditionReportView, Serializable {

    private static final long serialVersionUID = 1L;

    public String id;
    public int tick;
    public String outcome = "";
    public String detail = "";
    public Cost gains = Cost.ZERO;
    public int darkMatter;

    public ExpeditionReport() {
    }

    @Override public String id() { return id; }
    @Override public int tick() { return tick; }
    @Override public String outcome() { return outcome; }
    @Override public String detail() { return detail; }
    @Override public Cost gains() { return gains; }
    @Override public int darkMatter() { return darkMatter; }
}
