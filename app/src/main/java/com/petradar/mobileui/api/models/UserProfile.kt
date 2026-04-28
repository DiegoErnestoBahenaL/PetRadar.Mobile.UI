package com.petradar.mobileui.api.models

import com.google.gson.annotations.SerializedName

/**
 * Data models related to the user profile.
 *
 * Each class maps directly to a schema in the PetRadar Swagger:
 *  - [UserProfile]          → UserViewModel  (read)
 *  - [UpdateProfileRequest] → UserUpdateModel (write / partial update)
 */

/**
 * Full user profile data returned by GET /api/Users/{id}.
 * Maps to `UserViewModel` in the Swagger.
 *
 * @property id                  Unique user ID in the database.
 * @property email               Email address (also used as the username for login).
 * @property name                User first name.
 * @property lastName            Last name (may be null).
 * @property phoneNumber         Contact phone number (may be null).
 * @property profilePhotoURL     URL of the profile photo stored on the server (may be null).
 * @property role                User role: SuperAdmin | Admin | User | Organization | NotSet.
 * @property organizationName    Organization name if the user belongs to one (may be null).
 * @property organizationAddress Organization address (may be null).
 * @property organizationPhone   Organization phone number (may be null).
 */
data class UserProfile(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("email")
    val email: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("lastName")
    val lastName: String? = null,

    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,

    @SerializedName("profilePhotoURL")
    val profilePhotoURL: String? = null,

    @SerializedName("role")
    val role: String? = null,

    @SerializedName("organizationName")
    val organizationName: String? = null,

    @SerializedName("organizationAddress")
    val organizationAddress: String? = null,

    @SerializedName("organizationPhone")
    val organizationPhone: String? = null,

    @SerializedName("emailVerified")
    val emailVerified: Boolean = false
)

/**
 * Request body for updating a user's profile.
 * Maps to `UserUpdateModel` in the Swagger.
 *
 * All fields are optional (nullable). The server only updates the fields
 * that have a non-null value in the sent body.
 *
 * @property email             New email (optional).
 * @property password          New password (optional; never read back from the server).
 * @property name              New first name (optional).
 * @property lastName          New last name (optional).
 * @property phoneNumber       New phone number (optional).
 * @property organizationName  New organization name (optional).
 * @property organizationAddress New organization address (optional).
 * @property organizationPhone New organization phone number (optional).
 * @property role              New role (only admins should change this).
 */
data class UpdateProfileRequest(
    @SerializedName("email")
    val email: String? = null,

    @SerializedName("password")
    val password: String? = null,

    @SerializedName("name")
    val name: String? = null,

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

    @SerializedName("role")
    val role: String? = null
)
