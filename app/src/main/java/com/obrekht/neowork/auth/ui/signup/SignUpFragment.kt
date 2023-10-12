package com.obrekht.neowork.auth.ui.signup

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.net.toFile
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.obrekht.neowork.R
import com.obrekht.neowork.auth.ui.navigateToLogIn
import com.obrekht.neowork.databinding.FragmentSignUpBinding
import com.obrekht.neowork.utils.hideKeyboard
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpFragment : Fragment(R.layout.fragment_sign_up) {
    private val viewModel: SignUpViewModel by viewModels()
    private val binding by viewBinding(FragmentSignUpBinding::bind)

    private var formEditTextList = emptyList<TextInputEditText>()
    private var snackbar: Snackbar? = null

    private val attachAvatarLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                Activity.RESULT_OK -> {
                    val uri: Uri? = it.data?.data
                    viewModel.changeAvatar(uri, uri?.toFile())
                }
                ImagePicker.RESULT_ERROR -> {
                    Snackbar.make(
                        binding.root,
                        ImagePicker.getError(it.data),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

    override fun onStart() {
        super.onStart()
        if (viewModel.isLoggedIn) {
            findNavController().popBackStack()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding) {
            formEditTextList = listOf(
                nameEditText,
                usernameEditText,
                passwordEditText,
                passwordConfirmationEditText
            )

            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            avatar.setOnClickListener {
                ImagePicker.with(this@SignUpFragment)
                    .cropSquare()
                    .compress(2048)
                    .provider(ImageProvider.BOTH)
                    .galleryMimeTypes(
                        arrayOf(
                            "image/png",
                            "image/jpeg",
                        )
                    )
                    .createIntent(attachAvatarLauncher::launch)
            }

            val textChangedAction = { _: Editable? ->
                viewModel.validateFormData(
                    nameEditText.text.toString(),
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString(),
                    passwordConfirmationEditText.text.toString()
                )
            }

            formEditTextList.forEach { it.doAfterTextChanged(textChangedAction) }
            passwordConfirmationEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    signUp()
                }
                false
            }
            buttonSignUp.setOnClickListener {
                signUp()
            }
            buttonLogIn.setOnClickListener {
                navigateToLogIn()
            }
        }

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                viewModel.uiState.onEach { state ->
                    with(binding) {
                        if (state.isLoading) {
                            progressBar.show()
                            setInteractionsActive(false)
                        } else if(state.result !is SignUpResult.Success) {
                            buttonSignUp.isEnabled = state.formState.isDataValid
                        }

                        state.avatarModel.uri?.let {
                            avatar.setImageURI(it)
                        }

                        nameTextField.error = when (state.formState.nameError) {
                            NameError.Empty -> getString(R.string.error_empty_name)
                            else -> null
                        }

                        usernameTextField.error = when (state.formState.usernameError) {
                            UsernameError.Empty -> getString(R.string.error_empty_username)
                            else -> null
                        }

                        passwordTextField.error = when (state.formState.passwordError) {
                            PasswordError.Empty -> getString(R.string.error_empty_password)
                            else -> null
                        }

                        passwordConfirmationTextField.error =
                            when (state.formState.passwordConfirmationError) {
                                PasswordConfirmationError.DoNotMatch -> getString(R.string.error_passwords_do_not_match)
                                else -> null
                            }

                        state.result?.let { result ->
                            progressBar.hide()

                            when (result) {
                                SignUpResult.Success -> onSignedUp()
                                is SignUpResult.Error -> {
                                    setInteractionsActive(true)

                                    showErrorSnackbar(R.string.error_unknown)
                                }
                            }
                        }
                    }
                }.launchIn(this)
            }
        }
    }

    override fun onDestroyView() {
        formEditTextList = emptyList()
        snackbar = null
        super.onDestroyView()
    }

    private fun onSignedUp() {
        findNavController().popBackStack()
    }

    private fun signUp() {
        with(binding) {
            viewModel.signUp(
                usernameEditText.text.toString(),
                passwordEditText.text.toString(),
                nameEditText.text.toString()
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