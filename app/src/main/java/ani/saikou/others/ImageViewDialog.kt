package ani.saikou.others

import android.animation.ObjectAnimator
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import ani.saikou.*
import ani.saikou.databinding.BottomSheetImageBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.io.File

class ImageViewDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetImageBinding? = null
    private val binding get() = _binding!!

    private var reload = false
    private var _title: String? = null
    private var _image: FileUrl? = null

    var onReloadPressed: ((ImageViewDialog) -> Unit)? = null
    var trans: Transformation<File>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            _title = it.getString("title")?.replace(Regex("[\\\\/:*?\"<>|]"), "")
            reload = it.getBoolean("reload")
            _image = it.getSerialized("image")!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val (title, image) = (_title to _image)
        if (image == null || title == null) {
            dismiss()
            toastString("Errore nel ricevere Dati Immagine")
            return
        }
        if (reload) {
            binding.bottomImageReload.visibility = View.VISIBLE
            binding.bottomImageReload.setSafeOnClickListener {
                onReloadPressed?.invoke(this)
            }
        }

        var uri: Uri? = null

        binding.bottomImageSave.setOnClickListener {
            uri?.let {
                saveImageToDownloads(
                    title,
                    BitmapFactory.decodeFile(it.path, BitmapFactory.Options()),
                    requireActivity()
                )
            }
        }

        binding.bottomImageShare.setOnClickListener {
            uri?.let {
                shareImage(
                    title,
                    BitmapFactory.decodeFile(it.path, BitmapFactory.Options()),
                    requireContext()
                )
            }
        }

        binding.bottomImageTitle.text = title

        binding.bottomImageShare.setOnLongClickListener {
            openLinkInBrowser(image.url)
            true
        }


        Glide.with(this).download(GlideUrl(image.url) { image.headers })
            .override(Target.SIZE_ORIGINAL)
            .apply {
                val target = object : CustomViewTarget<SubsamplingScaleImageView, File>(binding.bottomImageView) {
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        toastString("Loading Image Failed")
                        binding.bottomImageProgress.visibility = View.GONE
                    }

                    override fun onResourceCleared(placeholder: Drawable?) {}

                    override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                        uri = Uri.fromFile(resource)
                        binding.bottomImageShare.isEnabled = true
                        binding.bottomImageSave.isEnabled = true

                        binding.bottomImageView.setImage(ImageSource.uri(uri!!))
                        ObjectAnimator.ofFloat(binding.bottomImageView, "alpha", 0f, 1f).setDuration(400L).start()
                        binding.bottomImageProgress.visibility = View.GONE
                    }
                }
                trans?.let {
                    transform(File::class.java, it)
                }
                into(target)
            }


    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    companion object {
        fun newInstance(title: String, image: FileUrl, showReload: Boolean = false) = ImageViewDialog().apply {
            arguments = Bundle().apply {
                putString("title", title)
                putBoolean("reload", showReload)
                putSerializable("image", image)
            }
        }

        fun newInstance(activity: FragmentActivity, title: String?, image: String?): Boolean {
            ImageViewDialog().apply {
                arguments = Bundle().apply {
                    putString("title", title ?: return false)
                    putSerializable("image", FileUrl(image ?: return false))
                }
                show(activity.supportFragmentManager, "image")
            }
            return true
        }
    }
}