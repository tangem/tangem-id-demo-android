package com.tangem.id.features.holder.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tangem.id.R
import com.tangem.id.common.entities.*
import com.tangem.id.common.extensions.show
import com.tangem.id.features.holder.redux.AccessLevel
import com.tangem.id.features.holder.redux.HolderAction
import com.tangem.id.features.holder.redux.HolderCredential
import kotlinx.android.synthetic.main.layout_holder_credential.view.*

class HolderCredentialsAdapter(
    private val itemClickListener: (HolderAction) -> Unit
) : RecyclerView.Adapter<HolderCredentialsAdapter.CredentialViewHolder>() {

    private var items: List<HolderCredential> = listOf()
    private var isEditActivated = false

    override fun getItemCount() = items.size

    fun setItems(
        credentials: List<HolderCredential>,
        isEditActivated: Boolean = false
    ) {
        items = credentials
        this.isEditActivated = isEditActivated
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CredentialViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_holder_credential, parent, false)
        return CredentialViewHolder(
            layout,
            itemClickListener
        )
    }

    override fun onBindViewHolder(holder: CredentialViewHolder, position: Int) {

        val holderCredential = items[position]
        val credential = holderCredential.credential
        val accessLevel = holderCredential.accessLevel
        val context = holder.ivAccess.context

        holder.tvCredential.text =
            when (credential) {
                is Passport -> context.getString(R.string.credential_personal_info)
                is Photo -> context.getString(R.string.credential_photo)
                is SecurityNumber -> context.getString(R.string.credential_ssn)
                is AgeOfMajority -> context.getString(R.string.credential_age_of_majority)
                is ImmunityPassport -> context.getString(R.string.credential_covid)
                is VCExpert -> context.getString(R.string.vc_expert)
                else -> "Unknown"
            }

        holder.ivDelete.show(isEditActivated)

        val accessLevelImage = when (accessLevel) {
            AccessLevel.Private -> ContextCompat.getDrawable(
                holder.ivAccess.context,
                R.drawable.ic_baseline_lock
            )
            AccessLevel.Public -> ContextCompat.getDrawable(
                holder.ivAccess.context,
                R.drawable.ic_lock_open
            )
        }
        holder.ivAccess.setImageDrawable(accessLevelImage)

        holder.tvCredential.setOnClickListener {
            itemClickListener(HolderAction.ShowCredentialDetails(credential))
        }

        holder.ivAccess.setOnClickListener {
            itemClickListener(HolderAction.ChangeCredentialAccessLevel(credential))
        }

        holder.ivDelete.setOnClickListener {
            itemClickListener(HolderAction.RemoveCredential(credential))
        }
    }


    class CredentialViewHolder(view: View, itemClickListener: (HolderAction) -> Unit) :
        RecyclerView.ViewHolder(view) {
        val tvCredential: TextView = view.tv_holder_credential

        val ivDelete: ImageView = view.iv_delete_credential
        val ivAccess: ImageView = view.iv_credential_visibility


    }

}
