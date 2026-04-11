package com.example.petradar.utils

import com.example.petradar.api.RetrofitClient

/**
 * Resolves pet image URLs to absolute API URLs.
 */
object PetImageUrlResolver {

    /**
     * Converts a relative API path (e.g. "Secured/Images/x.jpg") into an absolute URL.
     */
    fun toAbsoluteApiUrl(pathOrUrl: String?): String? {
        val value = pathOrUrl?.trim().orEmpty()
        if (value.isBlank()) return null
        if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) {
            return value
        }
        val base = RetrofitClient.BASE_URL.trimEnd('/')
        val normalizedPath = value.trimStart('/')
        return "$base/$normalizedPath"
    }

    /**
     * Deterministic endpoint that always points to the latest pet main picture.
     */
    fun mainPictureEndpoint(petId: Long): String {
        val base = RetrofitClient.BASE_URL.trimEnd('/')
        return "$base/api/UserPets/$petId/mainpicture"
    }

    /**
     * Deterministic endpoint that always points to the latest adoption animal main picture.
     */
    fun adoptionMainPictureEndpoint(animalId: Long): String {
        val base = RetrofitClient.BASE_URL.trimEnd('/')
        return "$base/api/AdoptionAnimals/$animalId/mainpicture"
    }

    /** URL for a specific additional photo of a UserPet. */
    fun petAdditionalPhotoUrl(petId: Long, photoName: String): String {
        val base = RetrofitClient.BASE_URL.trimEnd('/')
        return "$base/api/UserPets/$petId/additionalphotos/$photoName"
    }

    /** URL for a specific additional photo of an AdoptionAnimal. */
    fun adoptionAdditionalPhotoUrl(animalId: Long, photoName: String): String {
        val base = RetrofitClient.BASE_URL.trimEnd('/')
        return "$base/api/AdoptionAnimals/$animalId/additionalphotos/$photoName"
    }

    /** URL for a specific additional photo of a Report. */
    fun reportAdditionalPhotoUrl(reportId: Long, photoName: String): String {
        val base = RetrofitClient.BASE_URL.trimEnd('/')
        return "$base/api/Reports/$reportId/additionalphotos/$photoName"
    }
}

