package com.maintena.location.service;

import com.maintena.location.dto.request.LocationRequest;
import com.maintena.location.dto.response.LocationResponse;

import java.util.List;

public interface LocationService {
	List<LocationResponse> findAll();
	LocationResponse findById(Long id);
	LocationResponse create(LocationRequest request);
	LocationResponse update(Long id, LocationRequest request);
	void delete(Long id);
}
