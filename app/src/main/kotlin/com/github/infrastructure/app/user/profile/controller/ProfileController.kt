package com.github.infrastructure.app.user.profile.controller

import com.github.infrastructure.app.user.profile.ChangePasswordRequest
import com.github.infrastructure.app.user.profile.ProfileResponse
import com.github.infrastructure.app.user.profile.service.ProfileService
import com.github.infrastructure.app.user.profile.UpdateProfileRequest
import com.github.infrastructure.security.context.CurrentUserContext
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ProfileController(
    private val profileService: ProfileService,
) {
    @PatchMapping("/me")
    fun updateProfile(@Valid @RequestBody request: UpdateProfileRequest): ProfileResponse =
        profileService.updateProfile(request, CurrentUserContext.require())

    @PostMapping("/me/password")
    fun changePassword(@Valid @RequestBody request: ChangePasswordRequest) {
        profileService.changePassword(request, CurrentUserContext.require())
    }
}
