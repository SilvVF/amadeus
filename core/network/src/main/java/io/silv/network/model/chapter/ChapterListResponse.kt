
import io.silv.network.model.chapter.Chapter
import kotlinx.serialization.Serializable

@Serializable
data class ChapterListResponse(
    val result: String,
    val response: String,
    val data: List<Chapter> = emptyList(),
    val limit: Int,
    val offset: Int,
    val total: Int,
)