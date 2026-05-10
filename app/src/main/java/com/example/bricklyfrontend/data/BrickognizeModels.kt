package com.example.bricklyfrontend.data

data class ExternalSiteLink(
    val name: String,
    val url: String
)

data class BrickognizeItem(
    val id: String,
    val score: Float,
    val name: String?,
    val img_url: String?,
    val external_sites: List<ExternalSiteLink>?
)

data class BrickognizeResponse(
    val listing_id: String?,
    val items: List<BrickognizeItem>
)
