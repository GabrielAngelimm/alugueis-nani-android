package com.rentalvalidator.app.presentation.ui.tenants
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import com.rentalvalidator.app.domain.model.Tenant

@Composable
internal fun UnitFormHeader(isNew:Boolean,unitName:String?,onDismiss:()->Unit) {
    com.rentalvalidator.app.presentation.design.NaniHeader(if(isNew)"Nova unidade" else "Editar unidade",unitName) {
        IconButton(onClick=onDismiss){Icon(Icons.Rounded.Close,"Fechar")}
    }
}

@Composable
internal fun TenantFormHeader(tenant:Tenant?,onDismiss:()->Unit) {
    com.rentalvalidator.app.presentation.design.NaniHeader(if(tenant==null)"Novo inquilino" else "Editar inquilino",tenant?.name) {
        IconButton(onClick=onDismiss){Icon(Icons.Rounded.Close,"Fechar")}
    }
}

