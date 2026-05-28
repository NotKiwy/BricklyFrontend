package com.example.bricklyfrontend.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    @GET("/api/app/meetings/by_future")
    suspend fun getMeetingsByFuture(): Response<List<MeetingDefaultDTO>>

    @GET("/api/app/meetings/by_id/{id}")
    suspend fun getMeetingById(@Path("id") id: Long): Response<MeetingDefaultDTO>

    @GET("/api/app/meetings/types")
    suspend fun getMeetingTypes(): Response<List<MeetingTypeDefaultDTO>>

    @Multipart
    @POST("/api/app/meetings/create")
    suspend fun createMeeting(
        @Part("creatorId") creatorId: RequestBody,
        @Part("date") date: RequestBody,
        @Part("title") title: RequestBody,
        @Part("announceDate") announceDate: RequestBody,
        @Part("duration") duration: RequestBody,
        @Part("address") address: RequestBody,
        @Part("typeId") typeId: RequestBody,
        @Part("ticketPrice") ticketPrice: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("discountDuration") discountDuration: RequestBody?,
        @Part("discountAmount") discountAmount: RequestBody?,
        @Part("discountModifier") discountModifier: RequestBody?,
        @Part previewImage: MultipartBody.Part
    ): Response<MeetingDefaultDTO>

    @POST("/api/app/tickets/create")
    suspend fun createTicket(@Body dto: TicketCreateDTO): Response<TicketDefaultDTO>

    @GET("/api/app/tickets/by_user_id/{userId}")
    suspend fun getTicketsByUserId(@Path("userId") userId: Long): Response<List<TicketDefaultDTO>>

    @GET("/api/app/tickets/by_meeting_id/{meetingId}")
    suspend fun getTicketsByMeetingId(@Path("meetingId") meetingId: Long): Response<List<TicketDefaultDTO>>

    @GET("/api/app/meetings/by_creator_id/{creatorId}")
    suspend fun getMeetingsByCreatorId(
        @Path("creatorId") creatorId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PagedModelMeetingShortDTO>

    @PUT("/api/app/meetings/update/{id}")
    suspend fun updateMeeting(
        @Path("id") id: Long,
        @Body dto: MeetingUpdateDTO
    ): Response<MeetingDefaultDTO>

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

    @GET("/api/app/listings/paginated")
    suspend fun getListingsPaginated(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedModelListingDefaultDTO>

    @GET("/api/app/listings/by_status/{status}")
    suspend fun getListingsByStatus(
        @Path("status") status: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedModelListingDefaultDTO>

    @GET("/api/app/listings/by_id/{id}")
    suspend fun getListingById(@Path("id") id: Long): Response<ListingDefaultDTO>

    @Multipart
    @POST("/api/app/listings/create")
    suspend fun createListing(
        @Part("sellerId") sellerId: RequestBody,
        @Part("itemType") itemType: RequestBody,
        @Part("itemId") itemId: RequestBody,
        @Part("quantity") quantity: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("condition") condition: RequestBody,
        @Part("conditionRate") conditionRate: RequestBody,
        @Part("price") price: RequestBody,
        @Part("status") status: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): Response<ListingDefaultDTO>

    @GET("/api/app/sets/by_id/{id}")
    suspend fun getSetById(@Path("id") id: String): Response<BrickSetDTO>

    @GET("/api/app/inventories/sets/containing_part/{partId}")
    suspend fun getSetsContainingPart(
        @Path("partId") partId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedModelSetContainingBlPartDTO>

    @GET("/app/api/inventories/minifigs/containing_part/{partId}")
    suspend fun getMinigifsContainingPart(
        @Path("partId") partId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedModelMinifigContainingBlPartDTO>

    @GET("/api/app/inventories/parts/from_set/{setId}")
    suspend fun getPartsFromSet(
        @Path("setId") setId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedModelPartFromItemDTO>

    @GET("/app/api/inventories/minifigs/from_set/{setId}")
    suspend fun getMinigifsFromSet(
        @Path("setId") setId: String
    ): Response<List<MinifigFromSetDTO>>

    @GET("/api/app/inventories/sets/containing_minifig/{minifigId}")
    suspend fun getSetsContainingMinifig(
        @Path("minifigId") minifigId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedModelSetContainingBLMinifigDTO>

    @GET("/api/app/inventories/parts/from_minifig/{minifigId}")
    suspend fun getPartsFromMinifig(
        @Path("minifigId") minifigId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedModelPartFromItemDTO>

    @POST("/api/app/cart_items/create")
    suspend fun addCartItem(@Body dto: CartItemCreateDTO): Response<CartItemDefaultDTO>

    @GET("/api/app/minifigs/by_bl_id/{blId}")
    suspend fun getMinifigByBlId(@Path("blId") blId: String): Response<List<MinifigDTO>>

    @GET("/api/app/parts/by_bl_id/{blId}")
    suspend fun getPartByBlId(@Path("blId") blId: String): Response<List<PartDTO>>

    @GET("/api/app/listings/by_description_or_itemId_containing/{stringContained}")
    suspend fun searchListings(@Path("stringContained") query: String): Response<PagedModelListingDefaultDTO>

    @GET("/api/app/cart_items/by_user_id/{userId}")
    suspend fun getCartItemsByUserId(@Path("userId") userId: Long): Response<List<CartItemDefaultDTO>>

    @DELETE("/api/app/cart_items/delete/{id}")
    suspend fun deleteCartItem(@Path("id") id: Long): Response<Unit>

    @PUT("/api/app/cart_items/update/{id}")
    suspend fun updateCartItem(@Path("id") id: Long, @Body dto: CartItemUpdateDTO): Response<CartItemDefaultDTO>

    @GET("/api/app/listings/by_seller_id/{sellerId}")
    suspend fun getListingsBySellerId(
        @Path("sellerId") sellerId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PagedModelListingDefaultDTO>

    @PUT("/api/app/listings/update/{id}")
    suspend fun updateListing(
        @Path("id") id: Long,
        @Body dto: ListingUpdateDTO
    ): Response<ListingDefaultDTO>

    @POST("/api/app/payments/top_user_balance_up")
    suspend fun topUpBalance(@Body dto: TopUpRequestDTO): Response<TopUpResponseDTO>

    @GET("/api/app/payments/by_yookassa_id/{yooId}")
    suspend fun getPaymentByYooId(@Path("yooId") yooId: String): Response<TopUpResponseDTO>

    @POST("/api/app/payments/pay_for_cart")
    suspend fun payForCart(@Body dto: TopUpRequestDTO): Response<TopUpResponseDTO>
}

interface BrickognizeApiService {
    @Multipart
    @POST("/predict/")
    suspend fun predictPart(
        @Part image: MultipartBody.Part
    ): Response<BrickognizeResponse>
}
