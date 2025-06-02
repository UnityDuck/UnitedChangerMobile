package com.example.unitedchangermobile

data class User(
    val username: String,
    val password: String,
    val confirmPassword: String? = null
)
