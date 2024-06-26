package com.droidknights.app.feature.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidknights.app.core.domain.usecase.GetBookmarkedSessionsUseCase
import com.droidknights.app.feature.bookmark.model.BookmarkItemUiState
import com.droidknights.app.feature.bookmark.model.BookmarkUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val getBookmarkedSessionsUseCase: GetBookmarkedSessionsUseCase,
) : ViewModel() {

    private val _errorFlow = MutableSharedFlow<Throwable>()
    val errorFlow = _errorFlow.asSharedFlow()

    private val _bookmarkUiState = MutableStateFlow<BookmarkUiState>(BookmarkUiState.Loading)
    val bookmarkUiState = _bookmarkUiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                bookmarkUiState,
                getBookmarkedSessionsUseCase(),
            ) { bookmarkUiState, bookmarkSessions ->
                when (bookmarkUiState) {
                    is BookmarkUiState.Loading -> {
                        BookmarkUiState.Success(
                            isEditButtonSelected = false,
                            bookmarks = bookmarkSessions
                                .mapIndexed { index, session ->
                                    BookmarkItemUiState(
                                        index = index,
                                        session = session,
                                        isEditMode = false
                                    )
                                }
                                .toPersistentList()
                        )
                    }

                    is BookmarkUiState.Success -> {
                        bookmarkUiState.copy(
                            bookmarks = bookmarkSessions
                                .mapIndexed { index, session ->
                                    BookmarkItemUiState(
                                        index = index,
                                        session = session,
                                        isEditMode = bookmarkUiState.isEditButtonSelected
                                    )
                                }
                                .toPersistentList()
                        )
                    }
                }
            }.catch { throwable ->
                _errorFlow.emit(throwable)
            }.collect { _bookmarkUiState.value = it }
        }
    }

    fun clickEditButton() {
        val state = _bookmarkUiState.value
        if (state !is BookmarkUiState.Success) {
            return
        }

        _bookmarkUiState.value = state.copy(
            isEditButtonSelected = state.isEditButtonSelected.not(),
            bookmarks = state.bookmarks
                .map {
                    it.copy(isEditMode = !it.isEditMode.not())
                }
                .toPersistentList()
        )
    }
}
