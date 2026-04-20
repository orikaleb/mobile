package com.example.nexiride2.data.supabase

import com.example.nexiride2.data.supabase.dto.BookingPatchDto
import com.example.nexiride2.data.supabase.dto.BookingRowDto
import com.example.nexiride2.data.supabase.dto.CityRowDto
import com.example.nexiride2.data.supabase.dto.NotificationInsertDto
import com.example.nexiride2.data.supabase.dto.NotificationPatchDto
import com.example.nexiride2.data.supabase.dto.NotificationRowDto
import com.example.nexiride2.data.supabase.dto.PaymentMethodInsertDto
import com.example.nexiride2.data.supabase.dto.PaymentMethodRowDto
import com.example.nexiride2.data.supabase.dto.PaymentTransactionInsertDto
import com.example.nexiride2.data.supabase.dto.ProfilePatchDto
import com.example.nexiride2.data.supabase.dto.ProfileRowDto
import com.example.nexiride2.data.supabase.dto.RouteRowDto
import com.example.nexiride2.data.supabase.dto.RouteSeatInsertDto
import com.example.nexiride2.data.supabase.dto.RouteSeatPatchDto
import com.example.nexiride2.data.supabase.dto.RouteSeatRowDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Supabase [PostgREST](https://postgrest.org/) surface (auto-generated from Postgres tables).
 */
interface SupabasePostgrestApi {

    @GET("routes")
    suspend fun listAllRoutes(): List<RouteRowDto>

    @GET("routes")
    suspend fun routesByOrigin(@Query("origin") originEq: String): List<RouteRowDto>

    @GET("routes")
    suspend fun routesByDestination(@Query("destination") destinationEq: String): List<RouteRowDto>

    @GET("routes")
    suspend fun routeById(@Query("id") idEq: String): List<RouteRowDto>

    @GET("bookings")
    suspend fun bookingsByUser(@Query("user_id") userIdEq: String): List<BookingRowDto>

    @GET("bookings")
    suspend fun bookingById(@Query("id") idEq: String): List<BookingRowDto>

    @GET("cities")
    suspend fun listCities(): List<CityRowDto>

    @POST("routes")
    suspend fun insertRoutes(@Body body: List<RouteRowDto>): Response<Unit>

    @POST("cities")
    suspend fun insertCities(@Body body: List<CityRowDto>): Response<Unit>

    @POST("bookings")
    @Headers("Prefer: return=minimal")
    suspend fun insertBooking(@Body body: BookingRowDto): Response<Unit>

    @PATCH("bookings")
    @Headers("Prefer: return=representation")
    suspend fun patchBooking(
        @Query("id") idEq: String,
        @Body body: BookingPatchDto
    ): List<BookingRowDto>

    @GET("route_seats")
    suspend fun routeSeatsByRoute(@Query("route_id") routeIdEq: String): List<RouteSeatRowDto>

    @GET("route_seats")
    suspend fun routeSeatsByBooking(@Query("booking_id") bookingIdEq: String): List<RouteSeatRowDto>

    @POST("route_seats")
    @Headers("Prefer: return=minimal")
    suspend fun insertRouteSeats(@Body body: List<RouteSeatInsertDto>): Response<Unit>

    @PATCH("route_seats")
    @Headers("Prefer: return=representation")
    suspend fun patchRouteSeat(
        @Query("route_id") routeIdEq: String,
        @Query("seat_number") seatNumberEq: String,
        @Body body: RouteSeatPatchDto
    ): List<RouteSeatRowDto>

    @GET("profiles")
    suspend fun profileById(@Query("id") idEq: String): List<ProfileRowDto>

    @PATCH("profiles")
    @Headers("Prefer: return=representation")
    suspend fun patchProfile(
        @Query("id") idEq: String,
        @Body body: ProfilePatchDto
    ): List<ProfileRowDto>

    @GET("payment_methods")
    suspend fun paymentMethodsByUser(@Query("user_id") userIdEq: String): List<PaymentMethodRowDto>

    @POST("payment_methods")
    @Headers("Prefer: return=minimal")
    suspend fun insertPaymentMethod(@Body body: PaymentMethodInsertDto): Response<Unit>

    @DELETE("payment_methods")
    suspend fun deletePaymentMethod(@Query("id") idEq: String): Response<Unit>

    @POST("payment_transactions")
    @Headers("Prefer: return=minimal")
    suspend fun insertPaymentTransaction(@Body body: PaymentTransactionInsertDto): Response<Unit>

    @GET("notifications")
    suspend fun notificationsByUser(@Query("user_id") userIdEq: String): List<NotificationRowDto>

    @POST("notifications")
    @Headers("Prefer: return=minimal")
    suspend fun insertNotification(@Body body: NotificationInsertDto): Response<Unit>

    @PATCH("notifications")
    @Headers("Prefer: return=representation")
    suspend fun patchNotification(
        @Query("id") idEq: String,
        @Body body: NotificationPatchDto
    ): List<NotificationRowDto>
}
