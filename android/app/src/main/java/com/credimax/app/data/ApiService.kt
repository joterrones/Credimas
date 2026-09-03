package com.credimax.app.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("clients")
    suspend fun clients(@Query("q") q: String? = null): List<ClientDto>

    @GET("clients/{id}")
    suspend fun client(@Path("id") id: String): ClientDto

    @POST("clients")
    suspend fun createClient(@Body body: ClientBody): ClientDto

    @PUT("clients/{id}")
    suspend fun updateClient(@Path("id") id: String, @Body body: ClientBody): ClientDto

    @GET("quotes")
    suspend fun quotes(
        @Query("estado") estado: String? = null,
        @Query("clientId") clientId: String? = null,
    ): List<QuoteDto>

    @GET("quotes/{id}")
    suspend fun quote(@Path("id") id: String): QuoteDto

    @POST("quotes")
    suspend fun createQuote(@Body body: QuoteBody): QuoteDto

    @PUT("quotes/{id}")
    suspend fun updateQuote(@Path("id") id: String, @Body body: QuoteBody): QuoteDto

    @POST("quotes/{id}/reject")
    suspend fun rejectQuote(@Path("id") id: String): QuoteDto

    @Multipart
    @POST("quotes/{id}/convert")
    suspend fun convertQuote(
        @Path("id") id: String,
        @Part("fechaInicio") fechaInicio: RequestBody,
        @Part imagen: MultipartBody.Part? = null,
    ): LoanDto

    @GET("loans")
    suspend fun loans(
        @Query("estado") estado: String? = null,
        @Query("clientId") clientId: String? = null,
    ): List<LoanDto>

    @GET("loans/{id}")
    suspend fun loan(@Path("id") id: String): LoanDto

    @Multipart
    @POST("loans")
    suspend fun createLoan(
        @Part("clientId") clientId: RequestBody,
        @Part("capital") capital: RequestBody,
        @Part("interesPct") interesPct: RequestBody,
        @Part("semanas") semanas: RequestBody,
        @Part("fechaInicio") fechaInicio: RequestBody?,
        @Part imagen: MultipartBody.Part?,
    ): LoanDto

    @Multipart
    @POST("loans/{loanId}/installments/{installmentId}/pay")
    suspend fun payInstallment(
        @Path("loanId") loanId: String,
        @Path("installmentId") installmentId: String,
        @Part comprobante: MultipartBody.Part?,
        @Part("notas") notas: RequestBody?,
        @Part("fechaPago") fechaPago: RequestBody?,
        @Part("recargo") recargo: RequestBody?,
    ): PayResponse

    @GET("reports/dashboard")
    suspend fun dashboard(): DashboardDto

    @GET("reports/loans")
    suspend fun reportLoans(@Query("estado") estado: String? = null): List<LoanDto>

    @GET("reports/quotes")
    suspend fun reportQuotes(): QuotesReportDto

    @GET("preview/installments")
    suspend fun previewInstallments(
        @Query("capital") capital: Double,
        @Query("interesPct") interesPct: Double,
        @Query("semanas") semanas: Int,
        @Query("fechaInicio") fechaInicio: String? = null,
    ): PreviewDto

    @GET("reports/cartera")
    suspend fun reportCartera(): CarteraReportDto

    @GET("settings")
    suspend fun settings(): SettingsDto

    @PUT("settings")
    suspend fun updateSettings(@Body body: SettingsBody): SettingsDto
}
