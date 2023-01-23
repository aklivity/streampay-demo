/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.demo.streampay.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(
    ignoreUnknown = true
)
public interface Command
{
}
