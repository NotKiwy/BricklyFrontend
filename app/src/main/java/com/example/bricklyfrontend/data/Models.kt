package com.example.bricklyfrontend.data

data class UserCreateDTO(
    val username: String,
    val password: String,
    val createdAt: String
)

data class UserUpdateDTO(
    val username: String? = null,
    val name: String? = null,
    val password: String? = null,
    val email: String? = null,
    val city: String? = null
)

data class UserDefaultDTO(
    val id: Long,
    val username: String,
    val name: String?,
    val createdAt: String?,
    val email: String?,
    val city: String?,
    val balance: Int?,
    val cartItems: List<CartItemDefaultDTO>?,
    val authorities: List<AuthorityShortDTO>?
)

data class AuthorityShortDTO(
    val authority: String
)

data class UserFullDTO(
    val id: Long,
    val username: String,
    val name: String?,
    val password: String?,
    val createdAt: String?,
    val email: String?,
    val city: String?
)

data class UserShortDTO(
    val id: Long,
    val username: String,
    val name: String?
)

data class FeedbackDefaultDTO(
    val id: Long,
    val target_id: Long,
    val author: UserShortDTO?,
    val rate: Int,
    val comment: String?
)

data class FeedbackCreateDTO(
    val target_id: Long,
    val author: UserShortDTO,
    val rate: Int,
    val comment: String?
)

data class MeetingTypeDefaultDTO(
    val id: Int,
    val description: String?
)

data class MeetingDefaultDTO(
    val id: Long,
    val date: String?,
    val title: String?,
    val announceDate: String?,
    val previewImagePath: String?,
    val duration: Int?,
    val address: String?,
    val type: MeetingTypeDefaultDTO?,
    val ticketPrice: Int?,
    val description: String?,
    val discountDuration: Int?,
    val discountAmount: Int?,
    val discountModifier: Int?
)

data class PageMetadata(
    val size: Long,
    val number: Long,
    val totalElements: Long,
    val totalPages: Long
)

data class PagedModelMeetingDefaultDTO(
    val content: List<MeetingDefaultDTO>?,
    val page: PageMetadata?
)

data class FeedbackUpdateDTO(
    val rate: Int? = null,
    val comment: String? = null
)

data class MeetingShortDTO(
    val id: Long,
    val date: String?,
    val address: String?,
    val description: String?,
    val type: MeetingTypeDefaultDTO?
)

data class TicketDefaultDTO(
    val id: Long,
    val user: UserShortDTO?,
    val meeting: MeetingShortDTO?,
    val pricePaid: Int?,
    val state: Int?
)

data class TicketCreateDTO(
    val userId: Long,
    val meetingId: Long,
    val pricePaid: Int,
    val state: Int = 0
)

data class ListingImageDefaultDTO(
    val id: Long,
    val positionId: Int,
    val imagePath: String?
)

data class ListingDefaultDTO(
    val id: Long,
    val seller: UserShortDTO?,
    val status: String?,
    val itemType: String?,
    val itemId: String?,
    val quantity: Int?,
    val description: String?,
    val condition: String?,
    val conditionRate: Int?,
    val price: Int?,
    val viewsCount: Int?,
    val listingImage: List<ListingImageDefaultDTO>?
)

data class CartItemDefaultDTO(
    val id: Long,
    val userId: Long,
    val itemType: String?,
    val itemId: Int?,
    val quantity: Int?
)

data class PagedModelListingDefaultDTO(
    val content: List<ListingDefaultDTO>?,
    val page: PageMetadata?
)

data class ListingCreateDTO(
    val sellerId: Long,
    val itemType: String,
    val itemId: String,
    val quantity: Int,
    val description: String?,
    val condition: String,
    val conditionRate: Int,
    val price: Int,
    val status: String
)

data class CartItemCreateDTO(
    val userId: Long,
    val itemType: String,
    val itemId: Long,
    val quantity: Int
)

data class CartItemUpdateDTO(val quantity: Int)

data class ListingUpdateDTO(
    val status: String,
    val quantity: Int,
    val description: String,
    val price: Int,
    val viewsCount: Int
)

data class BlMinifigDTO(
    val id: String,
    val name: String?,
    val categoryName: String?,
    val imageUrl: String?,
    val url: String?
)

data class MinifigDTO(
    val id: String,
    val name: String?,
    val numParts: Int?,
    val imageUrl: String?,
    val blMinifig: BlMinifigDTO?
)

data class BrickSetThemeDTO(
    val id: Int,
    val name: String?
)

data class BrickSetDTO(
    val id: String,
    val name: String?,
    val year: Int?,
    val theme: BrickSetThemeDTO?,
    val numParts: Int?,
    val imageUrl: String?
)

data class TopUpRequestDTO(
    val userId: Long,
    val amount: String,
    val paymentToken: String
)

data class TopUpResponseDTO(
    val paymentId: String?,
    val status: String?,
    val confirmationUrl: String?,
    val cancellationReason: String?
)

data class PartCategoryDTO(
    val name: String?
)

data class BlPartDTO(
    val id: String,
    val imageUrl: String?
)

data class PartDTO(
    val id: String,
    val name: String?,
    val category: PartCategoryDTO?,
    val blPart: BlPartDTO?
)
