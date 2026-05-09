package com.example.bricklyfrontend.data

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("/api/app/users/register")
    suspend fun registerUser(@Body dto: UserCreateDTO): Response<UserDefaultDTO>

    @GET("/api/app/users/by_id/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<UserDefaultDTO>

    @GET("/api/app/users/by_username/{username}")
    suspend fun getUserByUsername(@Path("username") username: String): Response<UserDefaultDTO>

    @GET("/api/app/users/exists/{username}")
    suspend fun checkUserExistence(@Path("username") username: String): Response<String>

    @PUT("/api/app/users/update/{id}")
    suspend fun updateUser(
        @Path("id") id: Long,
        @Body dto: UserUpdateDTO
    ): Response<UserFullDTO>

    @GET("/api/app/meetings/paginated")
    suspend fun getMeetingsPaginated(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedModelMeetingDefaultDTO>

    @GET("/api/app/meetings")
    suspend fun getAllMeetings(): Response<List<MeetingDefaultDTO>>

    @GET("/api/app/meetings/by_id/{id}")
    suspend fun getMeetingById(@Path("id") id: Long): Response<MeetingDefaultDTO>

    @GET("/api/app/meetings/types")
    suspend fun getMeetingTypes(): Response<List<MeetingTypeDefaultDTO>>

    @POST("/api/app/meetings/create")
    suspend fun createMeeting(@Body dto: MeetingCreateDTO): Response<MeetingDefaultDTO>

    @POST("/api/app/tickets/create")
    suspend fun createTicket(@Body dto: TicketCreateDTO): Response<TicketDefaultDTO>

    @GET("/api/app/tickets/by_user_id/{userId}")
    suspend fun getTicketsByUserId(@Path("userId") userId: Long): Response<List<TicketDefaultDTO>>

    @GET("/api/app/feedbacks/by_target_id/{targetId}")
    suspend fun getFeedbacksByTargetId(
        @Path("targetId") targetId: Long
    ): Response<List<FeedbackDefaultDTO>>

    @GET("/api/app/feedbacks/by_author_id/{authorId}")
    suspend fun getFeedbacksByAuthorId(
        @Path("authorId") authorId: Long
    ): Response<List<FeedbackDefaultDTO>>

    @POST("/api/app/feedbacks/create")
    suspend fun createFeedback(@Body dto: FeedbackCreateDTO): Response<FeedbackDefaultDTO>

    @PUT("/api/app/feedbacks/update/{id}")
    suspend fun updateFeedback(
        @Path("id") id: Long,
        @Body dto: FeedbackUpdateDTO
    ): Response<FeedbackDefaultDTO>

    @DELETE("/api/app/feedbacks/delete/{id}")
    suspend fun deleteFeedback(@Path("id") id: Long): Response<Unit>
}

// Brickognize API — отдельный интерфейс (другой base url)
interface BrickognizeApiService {
    @Multipart
    @POST("v1/predict")
    suspend fun predictPart(
        @Part image: MultipartBody.Part
    ): Response<BrickognizeResponse>
}
