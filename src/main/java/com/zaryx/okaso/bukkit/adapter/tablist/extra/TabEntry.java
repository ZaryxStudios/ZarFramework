package com.zaryx.okaso.bukkit.adapter.tablist.extra;

public class TabEntry {

    private String text;
    private int ping;

    private String value, signature;

    public TabEntry(String text, int ping) {
        this.text = text;
        this.ping = ping;
    }

    public TabEntry(String text, int ping, String value, String signature) {
        this.text = text;
        this.ping = ping;
        this.value = value;
        this.signature = signature;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPing() {
        return this.ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSignature() {
        return this.signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
