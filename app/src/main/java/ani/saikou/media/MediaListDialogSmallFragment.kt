package ani.saikou.media

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputFilter.LengthFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import ani.saikou.*
import ani.saikou.anilist.Anilist
import ani.saikou.databinding.BottomSheetMediaListSmallBinding
import ani.saikou.others.getSerialized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable


class MediaListDialogSmallFragment : BottomSheetDialogFragment() {

    private lateinit var media: Media

    companion object {
        fun newInstance(m: Media): MediaListDialogSmallFragment =
            MediaListDialogSmallFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("media", m as Serializable)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            media = it.getSerialized("media")!!
        }
    }

    private var _binding: BottomSheetMediaListSmallBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetMediaListSmallBinding.inflate(inflater, container, false)
        return binding.root
    }


    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.mediaListContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += navBarHeight }
        val scope = viewLifecycleOwner.lifecycleScope

        binding.mediaListProgressBar.visibility = View.GONE
        binding.mediaListLayout.visibility = View.VISIBLE

        val statuses: Array<String> = resources.getStringArray(R.array.status).toList().map {
            when(it){
                "CURRENT" -> "GUARDANDO"
                "PLANNING" -> "DA VEDERE"
                "COMPLETED" -> "COMPLETATO"
                "DROPPED" -> "ABBANDONATO"
                "PAUSED" -> "IN PAUSA"
                "REPEATING" -> "RIGUARDANDO"
                else -> ""
            }}.toTypedArray()



        binding.mediaListStatus.setText(if (media.userStatus != null) media.userStatus else statuses[0])
        binding.mediaListStatus.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown,
                statuses
            )
        )


        var total: Int? = null
        binding.mediaListProgress.setText(if (media.userProgress != null) media.userProgress.toString() else "")
        if (media.anime != null) if (media.anime!!.totalEpisodes != null) {
            total = media.anime!!.totalEpisodes!!;binding.mediaListProgress.filters =
                arrayOf(
                    InputFilterMinMax(0.0, total.toDouble(), binding.mediaListStatus),
                    LengthFilter(total.toString().length)
                )
        } else if (media.manga != null) if (media.manga!!.totalChapters != null) {
            total = media.manga!!.totalChapters!!;binding.mediaListProgress.filters =
                arrayOf(
                    InputFilterMinMax(0.0, total.toDouble(), binding.mediaListStatus),
                    LengthFilter(total.toString().length)
                )
        }
        binding.mediaListProgressLayout.suffixText = " / ${total ?: '?'}"
        binding.mediaListProgressLayout.suffixTextView.updateLayoutParams {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.mediaListProgressLayout.suffixTextView.gravity = Gravity.CENTER

        binding.mediaListScore.setText(
            if (media.userScore != 0) media.userScore.div(
                10.0
            ).toString() else ""
        )
        binding.mediaListScore.filters =
            arrayOf(InputFilterMinMax(1.0, 10.0), LengthFilter(10.0.toString().length))
        binding.mediaListScoreLayout.suffixTextView.updateLayoutParams {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.mediaListScoreLayout.suffixTextView.gravity = Gravity.CENTER

        binding.mediaListIncrement.setOnClickListener {
            if (binding.mediaListStatus.text.toString() == statuses[0]) binding.mediaListStatus.setText(
                statuses[1],
                false
            )
            val init =
                if (binding.mediaListProgress.text.toString() != "") binding.mediaListProgress.text.toString()
                    .toInt() else 0
            if (init < (total ?: 5000)) binding.mediaListProgress.setText((init + 1).toString())
            if (init + 1 == (total ?: 5000)) {
                binding.mediaListStatus.setText(statuses[2], false)
            }
        }

        binding.mediaListPrivate.isChecked = media.isListPrivate
        binding.mediaListPrivate.setOnCheckedChangeListener { _, checked ->
            media.isListPrivate = checked
        }

        binding.mediaListSave.setOnClickListener {
            scope.launch {
                withContext(Dispatchers.IO) {
                    Anilist.mutation.editList(
                        media.id,
                        if (_binding?.mediaListProgress?.text.toString() != "") _binding?.mediaListProgress?.text.toString()
                            .toInt() else null,
                        if (_binding?.mediaListScore?.text.toString() != "") (_binding?.mediaListScore?.text.toString()
                            .toDouble() * 10).toInt() else null,
                        null,
                        if (_binding?.mediaListStatus?.text.toString() != "") when(_binding?.mediaListStatus?.text.toString()){
                            "GUARDANDO" -> "CURRENT"
                            "DA VEDERE" -> "PLANNING"
                            "COMPLETATO" -> "COMPLETED"
                            "ABBANDONATO" -> "DROPPED"
                            "IN PAUSA" -> "PAUSED"
                            "RIGUARDANDO" -> "REPEATING"
                            else -> ""
                        } else null,
                        media.isListPrivate
                    )
                }
                Refresh.all()
                toastString("Lista Aggiornata")
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
