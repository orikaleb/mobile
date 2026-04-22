package com.example.nexiride2.presentation.admin

/**
 * Admin allow-list. Users whose auth email matches one of these entries
 * (case-insensitive) can see the Admin tile in Profile and open the Admin page.
 *
 * Update this list to grant access to additional admins.
 */
object AdminConfig {
    val adminEmails: Set<String> = setOf(
        "admin@nexiride.com",
        "kalebotchere@gmail.com"
    )

    fun isAdmin(email: String?): Boolean =
        email?.trim()?.lowercase()?.let { adminEmails.map(String::lowercase).contains(it) } == true
}
