package com.example.web;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Request body for POST /order. */
public class OrderRequest {

    @NotBlank
    @Size(max = 100)
    private String item;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }
}
