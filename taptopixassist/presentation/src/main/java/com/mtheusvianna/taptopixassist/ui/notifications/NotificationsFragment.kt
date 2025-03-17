package com.mtheusvianna.taptopixassist.ui.notifications

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mtheusvianna.taptopixassist.presentation.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val notificationsViewModel: NotificationsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        notificationsViewModel.run {
            text.observe(viewLifecycleOwner) {
                binding.textNotifications.text = it
            }
            iconDrawable.observe(viewLifecycleOwner) {
                binding.contactlessIcon.setImageResource(it)
            }
            iconColor.observe(viewLifecycleOwner) {
                binding.contactlessIcon.setColorFilter(requireActivity().getColor(it), PorterDuff.Mode.SRC_IN)
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}