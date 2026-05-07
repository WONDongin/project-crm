package com.project.crm.domain.code;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CodeId implements Serializable {

    private String codeGroup;
    private String codeVal;
}