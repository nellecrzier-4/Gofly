package com.nellecrzier.gofileexpressive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pembungkus [GofileApi] untuk lapisan ViewModel: semua panggilan jaringan
 * dipindah ke [Dispatchers.IO] dan error tetap berupa [GoFileException].
 */
class GofileRepository {

    suspend fun sendLoginLink(email: String) = io { GofileApi.sendLoginLink(email) }

    suspend fun createGuest(): GSession = io { GofileApi.createGuest() }

    suspend fun getAccount(token: String): GSession = io { GofileApi.getAccount(token) }

    suspend fun getAccountDetails(token: String, accountId: String): org.json.JSONObject =
        io { GofileApi.getAccountDetails(token, accountId) }

    suspend fun getFolderPage(
        token: String,
        folderId: String,
        page: Int = 1,
        pageSize: Int = GofileConfig.PAGE_SIZE,
        sortField: String = "name",
        sortDirection: Int = 1,
        filter: String? = null,
    ): FolderPage = io {
        GofileApi.getFolderPage(token, folderId, page, pageSize, sortField, sortDirection, filter)
    }

    suspend fun search(token: String, folderId: String, query: String): List<GItem> =
        io { GofileApi.searchContents(token, folderId, query) }

    suspend fun createFolder(token: String, parentId: String, name: String) =
        io { GofileApi.createFolder(token, parentId, name) }

    suspend fun rename(token: String, contentId: String, newName: String) =
        io { GofileApi.renameContent(token, contentId, newName) }

    suspend fun delete(token: String, contentIds: List<String>) =
        io { GofileApi.deleteContents(token, contentIds) }

    suspend fun move(token: String, contentIds: List<String>, destinationId: String) =
        io { GofileApi.moveContents(token, contentIds, destinationId) }

    suspend fun copy(token: String, contentIds: List<String>, destinationId: String) =
        io { GofileApi.copyContents(token, contentIds, destinationId) }

    suspend fun updateAttribute(token: String, contentId: String, attribute: String, value: Any?) =
        io { GofileApi.updateAttribute(token, contentId, attribute, value) }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}
