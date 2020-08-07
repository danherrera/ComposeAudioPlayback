package com.example.audioplaybackcompose.profile

sealed class ProfileAction {
  data class FirstNameTextChanged(val firstName: String): ProfileAction()
  data class LastNameTextChanged(val lastName: String): ProfileAction()
}