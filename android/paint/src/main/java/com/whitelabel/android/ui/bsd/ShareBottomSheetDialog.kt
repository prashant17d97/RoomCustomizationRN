package com.whitelabel.android.ui.bsd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.whitelabel.android.databinding.FragmentBottomSheetDialogBinding

class ShareBottomSheetDialog(
    val onSaveToGalleryClick: () -> Unit,
    val onSaveToProjectClick: () -> Unit,
    val onSendToColorConsultationClick: () -> Unit,
    val onSharePhotoClick: () -> Unit
) : BottomSheetDialogFragment() {
    private var _binding: FragmentBottomSheetDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closeBtn.setOnClickListener {
            dismiss()
        }

        binding.saveToGallery.setOnClickListener {
            onSaveToGalleryClick()
        }
        binding.saveToProject.setOnClickListener {
            onSaveToProjectClick()
        }
        binding.shareWithConsultant.setOnClickListener {
            onSendToColorConsultationClick()
        }
        binding.sharePhoto.setOnClickListener {
            onSharePhotoClick()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}