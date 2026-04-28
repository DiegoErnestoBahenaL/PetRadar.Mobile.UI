package com.petradar.mobileui.api.models

import com.google.gson.annotations.SerializedName

/**
 * Data models related to user authentication.
 *
 * Each class maps directly to a schema defined in the PetRadar Swagger:
 *  - [LoginRequest]        → LoginModel
 *  - [LoginResponse]       → UserTokenViewModel
 *  - [RefreshTokenRequest] → RefreshTokenFromUiModel
 *  - [RegisterRequest]     → UserCreateModel
 *  - [ApiError]            → ProblemDetails
 */

/**
 * Request body for signing in.
 * Maps to `LoginModel` in the Swagger.
 *
 * @property username User's email (used as the username in PetRadar).
 * @property password Plain-text password; transmission is encrypted via HTTPS.
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)

/**
 * Server response on successful sign-in.
 * Maps to `UserTokenViewModel` in the Swagger.
 *
 * @property token                Short-lived JWT used to authenticate requests.
 * @property tokenValidTo         JWT expiry date/time (ISO-8601).
 * @property refreshToken         Long-lived token used to renew the JWT without re-login.
 * @property refreshTokenExpiryTime Refresh token expiry date/time (ISO-8601).
 */
data class LoginResponse(
    @SerializedName("token")
    val token: String?,

    @SerializedName("tokenValidTo")
    val tokenValidTo: String?,

    @SerializedName("refreshToken")
    val refreshToken: String?,

    @SerializedName("refreshTokenExpiryTime")
    val refreshTokenExpiryTime: String?
)

/**
 * Request body for renewing the JWT using the refresh token.
 * Maps to `RefreshTokenFromUiModel` in the Swagger.
 *
 * @property refreshToken The previously saved refresh token.
 */
data class RefreshTokenRequest(
    @SerializedName("refreshToken")
    val refreshToken: String
)

/**
 * Request body for registering a new user.
 * Maps to `UserCreateModel` in the Swagger.
 *
 * @property email             Unique user email (required).
 * @property password          Password (required; minimum length enforced by the server).
 * @property name              User first name (required).
 * @property lastName          Last name (optional).
 * @property phoneNumber       Contact phone number (optional).
 * @property organizationName  Organization name if applicable (optional).
 * @property organizationAddress Organization address (optional).
 * @property organizationPhone Organization phone number (optional).
 * @property role              User role; defaults to "User" for normal registrations.
 */
data class RegisterRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("lastName")
    val lastName: String? = null,

    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,

    @SerializedName("organizationName")
    val organizationName: String? = null,

    @SerializedName("organizationAddress")
    val organizationAddress: String? = null,

    @SerializedName("organizationPhone")
    val organizationPhone: String? = null,

    /** Role assigned to the new user. Regular users always use "User". */
    @SerializedName("role")
    val role: String = "User"
)

/**
 * Error structure returned by the API for 4xx/5xx responses.
 * Maps to `ProblemDetails` in the Swagger (RFC 7807).
 *
 * @property type     URI reference to the problem type (e.g. RFC 9110).
 * @property title    Short description of the error (e.g. "Unauthorized").
 * @property status   HTTP status code.
 * @property detail   Detailed description of the error (may be null).
 * @property instance URI of the specific problem instance (may be null).
 */
data class ApiError(
    @SerializedName("type")
    val type: String?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("status")
    val status: Int?,

    @SerializedName("detail")
    val detail: String?,

    @SerializedName("instance")
    val instance: String?
)

