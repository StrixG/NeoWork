package com.obrekht.neowork.auth.ui.login

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.obrekht.neowork.R
import com.obrekht.neowork.auth.model.InvalidUsernameOrPasswordException
import com.obrekht.neowork.auth.ui.navigateToSignUp
import com.obrekht.neowork.databinding.FragmentLoginBinding
import com.obrekht.neowork.utils.hideKeyboard
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.setBarsInsetsListener
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels()
    private val binding by viewBinding(FragmentLoginBinding::bind)

    private var formEditTextList = emptyList<TextInputEditText>()
    private var snackbar: Snackbar? = null

    override fun onStart() {
        super.onStart()
        if (viewModel.isLoggedIn) {
            findNavController().popBackStack()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding) {
            scrollView.setBarsInsetsListener { insets ->
                setPadding(
                    paddingLeft,
                    paddingTop,
                    paddingRight,
                    insets.bottom
                )
            }

            formEditTextList = listOf(usernameEditText, passwordEditText)

            appBar.statusBarForeground =
                MaterialShapeDrawable.createWithElevationOverlay(requireContext())

            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            val textChangedAction = { _: Editable? ->
                viewModel.validateFormData(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }
            formEditTextList.forEach { it.doAfterTextChanged(textChangedAction) }
            passwordEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    logIn()
                }
                false
            }
            buttonLogIn.setOnClickListener {
                logIn()
            }
            buttonSignUp.setOnClickListener {
                navigateToSignUp()
            }
        }

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                viewModel.uiState.onEach(::handleState).launchIn(this)
            }
        }
    }

    override fun onDestroyView() {
        formEditTextList = emptyList()
        snackbar = null
        super.onDestroyView()
    }

    private fun handleState(state: LoginUiState) = with(binding) {
        if (state.isLoading) {
            progressBar.show()
            setInteractionsActive(false)
        } else if (state.result !is LoginResult.Success) {
            buttonLogIn.isEnabled = state.formState.isDataValid
        }

        usernameTextField.error = when (state.formState.usernameError) {
            UsernameError.Empty -> getString(R.string.error_empty_username)
            else -> null
        }

        passwordTextField.error = when (state.formState.passwordError) {
            PasswordError.Empty -> getString(R.string.error_empty_password)
            else -> null
        }

        state.result?.let { result ->
            progressBar.hide()

            when (result) {
                LoginResult.Success -> onLoggedIn()
                is LoginResult.Error -> {
                    val message = when (result.error) {
                        is InvalidUsernameOrPasswordException -> {
                            R.string.error_invalid_username_or_password
                        }
                        else -> R.string.error_unknown
                    }
                    setInteractionsActive(true)
                    showErrorSnackbar(message)
                }
            }
            viewModel.resultHandled()
        }

        Unit
    }

    private fun onLoggedIn() {
        findNavController().popBackStack()
    }

    private fun logIn() {
        with(binding) {
            viewModel.logIn(
                usernameEditText.text.toString(),
                passwordEditText.text.toString()
            )
            passwordEditText.hideKeyboard()
        }
    }

    private fun setInteractionsActive(active: Boolean) {
        formEditTextList.forEach { it.isEnabled = active }
        with(binding) {
            buttonSignUp.isEnabled = active
            buttonLogIn.isEnabled = active
        }
    }

    private fun showErrorSnackbar(
        @StringRes resId: Int,
        duration: Int = Snackbar.LENGTH_LONG,
        action: View.OnClickListener? = null
    ) {
        snackbar = Snackbar.make(requireView(), resId, duration).apply {
            if (action != null) {
                setAction(R.string.retry, action)
            }
            show()
        }
    }
}
