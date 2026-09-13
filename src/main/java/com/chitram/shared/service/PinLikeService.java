package com.chitram.shared.service;

import com.chitram.recommendation.model.InteractionType;
import com.chitram.recommendation.service.RecommendationService;
import com.chitram.shared.repository.PinLikeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PinLikeService {

    private final PinLikeRepository pinLikeRepository;
    private final RecommendationService recommendationService;

    public PinLikeService(
            PinLikeRepository pinLikeRepository,
            RecommendationService recommendationService) {
        this.pinLikeRepository = pinLikeRepository;
        this.recommendationService = recommendationService;
    }

    @Transactional
    public LikeResult like(long userId, long pinId) {
        PinLikeRepository.LikeMutation mutation = pinLikeRepository.insertLike(userId, pinId);
        requirePin(mutation);
        if (mutation.changed()) {
            recommendationService.recordInteraction(userId, pinId, InteractionType.LIKE, null);
        }
        return new LikeResult(true, pinLikeRepository.countLikes(pinId));
    }

    @Transactional
    public LikeResult unlike(long userId, long pinId) {
        PinLikeRepository.LikeMutation mutation = pinLikeRepository.deleteLike(userId, pinId);
        requirePin(mutation);
        return new LikeResult(false, pinLikeRepository.countLikes(pinId));
    }

    private void requirePin(PinLikeRepository.LikeMutation mutation) {
        if (!mutation.pinExists()) {
            throw new IllegalArgumentException("Pin not found");
        }
    }

    public record LikeResult(boolean liked, long likeCount) {
    }
}
