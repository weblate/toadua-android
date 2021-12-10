package town.robin.toadua

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.navigation.fragment.findNavController
import town.robin.toadua.databinding.FragmentAuthBinding
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import town.robin.toadua.api.ToaduaService
import java.net.URI

class AuthFragment : Fragment() {
    private lateinit var binding: FragmentAuthBinding
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private val activityModel: ToaduaViewModel by activityViewModels {
        ToaduaViewModel.Factory(requireContext())
    }
    private val model: AuthViewModel by viewModels {
        AuthViewModel.Factory(activityModel.api, activityModel.prefs)
    }

    private enum class AuthType { SIGN_IN, CREATE_ACCOUNT }
    private var authType = AuthType.SIGN_IN

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAuthBinding.inflate(inflater, container, false)

        binding.authServer.text = URI(activityModel.prefs.server).host

        binding.continueButton.setOnClickListener {
            when (authType) {
                AuthType.SIGN_IN -> model::signIn
                AuthType.CREATE_ACCOUNT -> model::createAccount
            }.let { it(binding.usernameInput.text.toString(), binding.passwordInput.text.toString()) }
        }
        binding.createAccountButton.setOnClickListener {
            when (authType) {
                AuthType.SIGN_IN -> {
                    authType = AuthType.CREATE_ACCOUNT
                    binding.authTitle.text = getString(R.string.create_account)
                    binding.createAccountButton.text = getString(R.string.use_existing_account)
                }
                AuthType.CREATE_ACCOUNT -> {
                    authType = AuthType.SIGN_IN
                    binding.authTitle.text = getString(R.string.sign_in)
                    binding.createAccountButton.text = getString(R.string.create_account)
                }
            }
        }
        binding.changeServerButton.setOnClickListener {
            val input = EditText(requireContext()).apply {
                setText(activityModel.prefs.server)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ).apply {
                    val margin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
                    marginStart = margin
                    marginEnd = margin
                }
            }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.server_address)
                .setView(FrameLayout(requireContext()).apply { addView(input) })
                .setPositiveButton(R.string.confirm) { _, _ ->
                    val server = input.text.toString()
                    activityModel.prefs.server = server
                    binding.authServer.text = URI(server).host
                    activityModel.api = ToaduaService.create(server)
                    model.api = activityModel.api
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()

            input.postDelayed({ focusInput(input) },200)
        }
        binding.skipButton.setOnClickListener {
            findNavController().navigate(R.id.auth_to_search)
        }


        //new start
        val navigation = binding.mynavigationview
        navigation.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_language->                 // Handle menu click
                    true
                R.id.nav_glosser ->    {             // Handle settings click
                    findNavController().apply {
                        //item.setTitle("Search")
                        popBackStack()
                        navigate(R.id.gloss_fragment)
                    }
                    true}
                R.id.nav_search ->    {             // Handle settings click
                    findNavController().apply {
                        //getActivity()?.setTitle("Search")
                        popBackStack()
                        navigate(R.id.search_fragment)
                    }
                    true}

                R.id.nav_logout ->                 // Handle logout click
                    true

                else -> false
            }
        }
//new end


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.loading.collect {
                    binding.usernameInput.isEnabled = !it
                    binding.passwordInput.isEnabled = !it
                    binding.authLoadingIndicator.visibility = if (it) View.VISIBLE else View.GONE
                    binding.continueButton.isEnabled = !it
                    binding.createAccountButton.isEnabled = !it
                    binding.skipButton.isEnabled = !it
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.loggedIn.collect {
                    if (it) findNavController().navigate(R.id.auth_to_search)
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        val drawerLayout = binding.myDrawerLayout
        actionBarDrawerToggle =
            ActionBarDrawerToggle(activity, drawerLayout, R.string.nav_open , R.string.nav_close)

        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        actionBarDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)

    private fun focusInput(view: View) {
        view.requestFocus()
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}