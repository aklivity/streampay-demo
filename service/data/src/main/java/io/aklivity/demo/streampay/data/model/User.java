/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.demo.streampay.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class User
{
    private String id;
    private String name;
    private String username;
}