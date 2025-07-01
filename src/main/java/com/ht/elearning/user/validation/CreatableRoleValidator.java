package com.ht.elearning.user.validation;


import com.ht.elearning.user.Role;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CreatableRoleValidator implements ConstraintValidator<CreatableRole, Role> {
    @Override
    public boolean isValid(Role value, ConstraintValidatorContext context) {
        return value == null || value == Role.USER || value == Role.PROVIDER;
    }
}
