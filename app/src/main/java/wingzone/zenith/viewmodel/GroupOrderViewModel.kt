package wingzone.zenith.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import wingzone.zenith.data.models.*
import wingzone.zenith.data.repository.IAuthRepository
import wingzone.zenith.data.repository.GroupOrderRepository
import wingzone.zenith.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GroupOrderViewModel(
    private val authRepository: IAuthRepository = RepositoryProvider.getAuthRepository(),
    private val groupOrderRepository: GroupOrderRepository = RepositoryProvider.getGroupOrderRepository()
) : ViewModel() {
    
    val groupOrders: StateFlow<List<GroupOrder>> = groupOrderRepository.groupOrders
    val currentGroupOrder: StateFlow<GroupOrder?> = groupOrderRepository.currentGroupOrder
    
    fun createGroupOrder(
        deliveryAddress: String? = null,
        specialInstructions: String? = null,
        onResult: (Result<GroupOrder>) -> Unit
    ) {
        viewModelScope.launch {
            val result = groupOrderRepository.createGroupOrder(deliveryAddress, specialInstructions)
            onResult(result)
        }
    }
    
    fun joinGroupOrder(code: String, onResult: (Result<GroupOrder>) -> Unit) {
        viewModelScope.launch {
            val result = groupOrderRepository.joinGroupOrder(code)
            onResult(result)
        }
    }
    
    fun leaveGroupOrder(orderId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = groupOrderRepository.leaveGroupOrder(orderId)
            onResult(result)
        }
    }
    
    fun addItemToGroupOrder(orderId: String, cartItem: CartItem, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = groupOrderRepository.addItemToGroupOrder(orderId, cartItem)
            onResult(result)
        }
    }
    
    fun removeItemFromGroupOrder(orderId: String, userId: String, itemIndex: Int, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = groupOrderRepository.removeItemFromGroupOrder(orderId, userId, itemIndex)
            onResult(result)
        }
    }
    
    fun markMemberAsPaid(orderId: String, userId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = groupOrderRepository.markMemberAsPaid(orderId, userId)
            onResult(result)
        }
    }
    
    fun startOrdering(orderId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = groupOrderRepository.startOrdering(orderId)
            onResult(result)
        }
    }
    
    fun finalizeGroupOrder(orderId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = groupOrderRepository.finalizeGroupOrder(orderId)
            onResult(result)
        }
    }
    
    fun payForMember(orderId: String, memberId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = groupOrderRepository.payForMember(orderId, memberId)
            onResult(result)
        }
    }
    
    fun kickMember(orderId: String, memberId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = groupOrderRepository.kickMember(orderId, memberId)
            onResult(result)
        }
    }
    
    fun getUserGroupOrders(): List<GroupOrder> {
        return groupOrderRepository.getUserGroupOrders()
    }
    
    fun getGroupOrder(code: String): GroupOrder? {
        return groupOrderRepository.getGroupOrder(code)
    }
    
    fun setCurrentGroupOrder(order: GroupOrder?) {
        groupOrderRepository.setCurrentGroupOrder(order)
    }
    
    fun clearCurrentGroupOrder() {
        groupOrderRepository.setCurrentGroupOrder(null)
    }
    
    fun generateOrderCode(): String {
        return groupOrderRepository.generateOrderCode()
    }
    
    fun startListeningToGroupOrder(code: String, onUpdate: (GroupOrder?) -> Unit) {
        groupOrderRepository.startListeningToGroupOrder(code, onUpdate)
    }
}
