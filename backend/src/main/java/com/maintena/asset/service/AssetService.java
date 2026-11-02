package com.maintena.asset.service;

import com.maintena.asset.dto.request.AssetRequest;
import com.maintena.asset.dto.response.AssetResponse;

import java.util.List;

public interface AssetService {
	List<AssetResponse> findAll();
	AssetResponse findById(Long id);
	AssetResponse create(AssetRequest request);
	AssetResponse update(Long id, AssetRequest request);
	void delete(Long id);
}
