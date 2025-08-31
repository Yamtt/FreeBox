package io.knifer.freebox.handler.impl;

import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.handler.MovieSuggestionHandler;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.json.GsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.textfield.AutoCompletionBinding;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 搜片网影视建议处理器
 *
 * @author Knifer
 */
@Slf4j
public class SoupianMovieSuggestionHandler implements MovieSuggestionHandler {

    private static final String SEARCH_SUGGESTION_REQUEST_URL = "https://soupian.plus/prefix/search?wd=";
    private final String[] SEARCH_SUGGESTION_REQUEST_HEADERS = {
            HttpHeaders.USER_AGENT, BaseValues.USER_AGENT,
            HttpHeaders.REFERER, "https://soupian.plus/"
    };

    @Override
    public Collection<String> handle(AutoCompletionBinding.ISuggestionRequest suggestionRequest) {
        String userText = suggestionRequest.getUserText();
        String resultBody;
        JsonObject resultJsonObj;
        JsonElement resultJsonElm;


        if (StringUtils.isBlank(userText)) {
            return Collections.emptyList();
        }
        try {
            resultBody = HttpUtil.getAsync(
                    SEARCH_SUGGESTION_REQUEST_URL + userText, SEARCH_SUGGESTION_REQUEST_HEADERS
            ).get(6, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            log.warn("request error", e);

            return Collections.emptyList();
        }
        log.info("userText: {}, resultBody: {}", userText, resultBody);
        resultJsonObj = GsonUtil.fromJson(resultBody, JsonObject.class);
        if (resultJsonObj == null) {
            return Collections.emptyList();
        }
        resultJsonElm = resultJsonObj.get("g");
        if (resultJsonElm == null) {
            return Collections.emptyList();
        }

        return resultJsonElm.getAsJsonArray()
                .asList()
                .stream()
                .map(JsonElement::getAsString)
                .toList();
    }
}
