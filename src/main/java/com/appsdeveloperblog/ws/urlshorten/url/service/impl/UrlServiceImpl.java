package com.appsdeveloperblog.ws.urlshorten.url.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.UserProfileService;
import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import com.appsdeveloperblog.ws.urlshorten.url.event.ShortUrlClickedEvent;
import com.appsdeveloperblog.ws.urlshorten.url.exception.InvalidUrlException;
import com.appsdeveloperblog.ws.urlshorten.url.exception.UrlRetryException;
import com.appsdeveloperblog.ws.urlshorten.url.mapper.ShortUrlMapper;
import com.appsdeveloperblog.ws.urlshorten.url.messaging.ClickEventPublisher;
import com.appsdeveloperblog.ws.urlshorten.url.model.enums.UrlStatus;
import com.appsdeveloperblog.ws.urlshorten.url.model.request.CreateUrlRequest;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.CreateUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.ListUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.ShortUrlAnalyticResponse;
import com.appsdeveloperblog.ws.urlshorten.url.repository.UrlRepository;
import com.appsdeveloperblog.ws.urlshorten.url.service.RedirectCacheService;
import com.appsdeveloperblog.ws.urlshorten.url.service.UrlService;
import com.appsdeveloperblog.ws.urlshorten.url.util.ShortUrlUtil;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.appsdeveloperblog.ws.urlshorten.workplace.model.entity.Workplace;
import com.appsdeveloperblog.ws.urlshorten.workplace.repository.WorkplaceRepository;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {
  private final UrlRepository urlRepository;
  private final UserRepository userRepository;
  private final WorkplaceRepository workplaceRepository;
  private static final Integer MAX_ALLOWED_CHARACTERS = 2048;
  private static final String BASE62 =
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final int MAX_RETRY = 5;
  private final RedirectCacheService redirectCacheService;
  private final ClickEventPublisher clickEventPublisher;
  private final ShortUrlMapper shortUrlMapper;

  @Value("${app.base-url}")
  private String baseUrl;

  @Override
  public CreateUrlResponse createShortUrl(CreateUrlRequest request, UUID userId, UUID workplaceId) {
    String normalizedLongUrl = getNormalizedLongUrl(request.getLongUrl());
    for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
      String shortCode = generateRandomBase62(8);
      User currentUser = userRepository.findById(userId).orElse(null);
      Workplace currentWorkplace = workplaceRepository.findById(workplaceId).orElse(null);
      try {
        ShortUrl entity = buildShortUrlEntity(shortCode, normalizedLongUrl, currentWorkplace, currentUser);
        String shortUrl = ShortUrlUtil.buildShortUrl(baseUrl, shortCode);
        urlRepository.saveAndFlush(entity);

        return CreateUrlResponse.builder()
            .shortCode(shortCode)
            .shortUrl(shortUrl)
            .status(UrlStatus.ACTIVE.name())
            .validFrom(entity.getValidFrom().toString())
            .expiresAt(entity.getExpiresAt().toString())
            .build();
      } catch (DataIntegrityViolationException ex) {
        if (attempt == MAX_RETRY) {
          throw new UrlRetryException(request.getLongUrl());
        }
      }
    }

    throw new UrlRetryException(request.getLongUrl());
  }

  private String normalizeUrl(String url) {
    String trimmedUrl = url.trim();
    if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
      return "https://" + trimmedUrl;
    }

    return trimmedUrl;
  }

  private boolean isValidUri(URI uri) {
    String host = uri.getHost();
    if (!StringUtils.hasText(host) || !StringUtils.hasText(uri.getPath())) {
      return false;
    }

    if (host.equals("localhost")
        || host.equals("127.0.0.1")
        || host.startsWith("192.168.")
        || host.startsWith("10.")) {
      return false;
    }
    return true;
  }

  private String generateRandomBase62(int length) {
    StringBuffer sb = new StringBuffer();
    Random random = new Random();

    for (int i = 0; i < length; i++) {
      sb.append(BASE62.charAt(random.nextInt(BASE62.length())));
    }

    return sb.toString();
  }

  private ShortUrl buildShortUrlEntity(String shortCode, String normalizedLongUrl, Workplace workplace, User currentUser) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(30, ChronoUnit.DAYS);

    ShortUrl entity = new ShortUrl();
    entity.setShortCode(shortCode);
    entity.setLongUrl(normalizedLongUrl);
    entity.setStatus(UrlStatus.ACTIVE);
    entity.setValidFrom(now);
    entity.setExpiresAt(expiresAt);
    entity.setNumberOfClicks(0L);
    entity.setOwner(currentUser);
    entity.setWorkplace(workplace);
    return entity;
  }
  // 1. Naming và purpose: Hàm này có tên mơ hồ chung chung (không thể hiện resource trả về)
  // bản chất hàm này là làm sạch url rồi kiểm tra nó có valid hay không thôi
  // Tên mình đề xuất validateNormalizedLongUrl()
  // 2. Interface và parameter: (parameter thì chỉ có 1 biến (ổn) nhưng thực chất nó chỉ lấy mỗi longUrl thay vì tất cả properties trong object
  // thế nên lấy ra 1 properties thay vì truyền hẳn 1 object vào
  // Theo như kiến thức mà bạn nói thì hàm này có mức độ abstraction không đồng đều,sau khi tạo normalize nó còn validate normalize
  // trong hàm này có 2 lần validate 1 cái cho normalize còn 1 cái validate uri, bạn đánh giá cách handle exception này thế nào
  // 3. Defensive programming và xử lý lỗi:
  // + Không có sử dụng biến boolean (tốt)
  // + Parameter không vượt quá 7
  // + Validate input đầu vào (chưa làm, lỡ request rỗng hay longUrl null/rỗng thì sao)
  // + Xử lý lỗi, mình đang không biết review như thế nào?
  // 4. Control structure and complexity
  // + Nesting level: Không có câu điều kiện hay vòng lặp vượt quá 3
  // + Điểm quyết định: Ở đây có 2 điểm thôi nhỉ (đoạn  if (normalizedLongUrl.length() > MAX_ALLOWED_CHARACTERS) và if (!isValidUrl(uri))
  // + Luồng chính rõ ràng (nominal path): mình đang không biết nên review như thế nào
  // 5. Understanding, code comment and remove unused code
  // + Code comment chưa có

  private String getNormalizedLongUrl(String longUrl) {
    if(!StringUtils.hasLength(longUrl)) {
      throw new InvalidUrlException("Invalid long url: long url must not be empty or null");
    }
    String normalizedLongUrl = normalizeUrl(longUrl);

    if(!isValidUrl(normalizedLongUrl)) {
      throw new InvalidUrlException("Invalid URL format: Long URL must be a valid HTTP/HTTPS address and under 2048 characters.");
    }

    return normalizedLongUrl;
  }

  private boolean isValidUrl(String longUrl) {
    if (longUrl.length() > MAX_ALLOWED_CHARACTERS) {
//      "Exceed valid length: normalized long url exceed maximum allowed characters (2048)."
      return false;
    }

    try {
      URI uri = URI.create(longUrl);
      if (!isValidUri(uri)) {
//    "Invalid host url: normalized long url has invalid host."
        return false;
      }
    } catch (IllegalArgumentException e) {
//     "Uri creation error: cannot create url from normalized long url"
      return false;
    }

    return true;
  }

  @Override
  public Optional<String> getValidLongUrl(String shortCode) {
    Optional<String> cachedLongUrl = redirectCacheService.getLongUrl(shortCode);
    ShortUrlClickedEvent clickedEvent =
        new ShortUrlClickedEvent(UUID.randomUUID(), shortCode, Instant.now(), 1);
    if (cachedLongUrl.isPresent()) {
      clickEventPublisher.publish(clickedEvent);
      return cachedLongUrl;
    }
    ShortUrl shortUrl = urlRepository.getByShortCode(shortCode);

    if (shortUrl == null
        || shortUrl.getStatus() != UrlStatus.ACTIVE
        || !shortUrl.getExpiresAt().isAfter(Instant.now())) {
      return Optional.empty();
    }
    redirectCacheService.put(
        shortCode, shortUrl.getLongUrl(), shortUrl.getExpiresAt().toEpochMilli());
    clickEventPublisher.publish(clickedEvent);
    return Optional.of(shortUrl.getLongUrl());
  }

  @Override
  public ShortUrlAnalyticResponse getAnalyticShortUrl(UUID urlId) {
    ShortUrl response = urlRepository.getById(urlId);
    return new ShortUrlAnalyticResponse(response);
  }

  @Override
  public ListUrlResponse getValidShortUrls(Integer pageNumber, Integer pageSize) {
    Instant now = Instant.now();
    Pageable pageable = PageRequest.of(pageNumber, pageSize);
    Page<ShortUrl> shortUrlList = urlRepository.getValidShortUrls(now, pageable);
    if (shortUrlList.getTotalElements() == 0) {
      return new ListUrlResponse();
    }
    ListUrlResponse response = shortUrlMapper.mappingListUrlResponse(shortUrlList.getContent());
    return response;
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public int expireOutdatedUrls() {
    Instant now = Instant.now();
    return urlRepository.expireOutdatedUrls(now);
  }
}
