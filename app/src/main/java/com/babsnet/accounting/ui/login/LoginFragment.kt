package com.babsnet.accounting.ui.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.babsnet.accounting.R
import com.babsnet.accounting.databinding.FragmentLoginBinding


class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        requireActivity().findViewById<View>(R.id.nav_view)?.visibility = View.GONE
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

     
        binding.buttonSignIn.setOnClickListener {
            val phoneNumber = binding.editTextPhone.text.toString()
            if (phoneNumber.isNotEmpty()) {
                findNavController().navigate(R.id.action_loginFragment_to_home)
            } else {
                Toast.makeText(requireContext(), "Enter your phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
        _binding = null
    }
}