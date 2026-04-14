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
    val city: String?
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

data class MeetingCreateDTO(
    val date: String,
    val address: String,
    val typeId: Int,
    val ticketPrice: Int,
    val description: String?,
    val discountDuration: Int? = null,
    val discountAmount: Int? = null,
    val discountModifier: Int? = null
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
