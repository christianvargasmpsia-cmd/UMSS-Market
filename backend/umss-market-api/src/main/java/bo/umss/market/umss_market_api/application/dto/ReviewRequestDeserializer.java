package bo.umss.market.umss_market_api.application.dto;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
/** Reject Jackson's default float-to-int and scalar-to-string coercions for review input. */
public class ReviewRequestDeserializer extends JsonDeserializer<ReviewRequest> {
    @Override public ReviewRequest deserialize(JsonParser parser,DeserializationContext context) throws IOException {
        JsonNode node=parser.getCodec().readTree(parser);
        if (!node.isObject()) throw JsonMappingException.from(parser,"Se requiere un objeto JSON");
        JsonNode rating=node.path("rating"),comment=node.path("comment");
        if (rating.isValueNode() && !rating.isNull() && (!rating.isIntegralNumber() || !rating.canConvertToInt()))
            throw JsonMappingException.from(parser,"rating debe ser entero");
        if (comment.isValueNode() && !comment.isNull() && !comment.isTextual())
            throw JsonMappingException.from(parser,"comment debe ser texto");
        if (rating.isContainerNode() || comment.isContainerNode()) throw JsonMappingException.from(parser,"Campos escalares requeridos");
        return new ReviewRequest(rating.isIntegralNumber()?rating.intValue():null,comment.isTextual()?comment.textValue():null);
    }
}
