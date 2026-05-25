package com.pc.fash_android_mobile.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.uxsurvey.UxSurveyAnswerInput
import com.pc.fash_android_mobile.data.uxsurvey.UxSurveyDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserExperienceSurveyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as FashApplication).uxSurveyRepository

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _survey = MutableStateFlow<UxSurveyDetail?>(null)
    val survey: StateFlow<UxSurveyDetail?> = _survey.asStateFlow()

    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted.asStateFlow()

    private val ratings = mutableMapOf<String, Int>()
    private val texts = mutableMapOf<String, String>()

    fun load(surveyKey: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _submitted.value = false
            ratings.clear()
            texts.clear()
            val result = withContext(Dispatchers.IO) { repository.getSurvey(surveyKey) }
            _loading.value = false
            result.fold(
                onSuccess = { detail ->
                    _survey.value = detail
                    if (detail.submitted) _submitted.value = true
                },
                onFailure = { ex ->
                    _survey.value = null
                    _error.value = ex.message?.takeIf { it.isNotBlank() }
                },
            )
        }
    }

    fun setRating(questionId: String, rating: Int) {
        ratings[questionId] = rating.coerceIn(1, 5)
    }

    fun setText(questionId: String, text: String) {
        texts[questionId] = text
    }

    fun submit(onSuccess: () -> Unit, onFailure: () -> Unit) {
        val detail = _survey.value ?: return
        if (_submitting.value || _submitted.value) return
        viewModelScope.launch {
            _submitting.value = true
            val answers = detail.questions.mapNotNull { q ->
                when (q.questionType) {
                    "rating" -> {
                        val r = ratings[q.id] ?: return@mapNotNull if (q.required) null else return@mapNotNull null
                        UxSurveyAnswerInput(questionId = q.id, rating = r)
                    }
                    "text" -> {
                        val t = texts[q.id]?.trim().orEmpty()
                        if (t.isEmpty() && q.required) return@mapNotNull null
                        UxSurveyAnswerInput(questionId = q.id, text = t)
                    }
                    else -> null
                }
            }
            if (answers.size < detail.questions.count { it.required && it.questionType == "rating" }) {
                _submitting.value = false
                onFailure()
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                repository.submit(detail.surveyKey, answers)
            }
            _submitting.value = false
            result.fold(
                onSuccess = {
                    _submitted.value = true
                    onSuccess()
                },
                onFailure = { onFailure() },
            )
        }
    }
}
