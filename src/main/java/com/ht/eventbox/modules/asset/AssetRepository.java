package com.ht.eventbox.modules.asset;

import com.ht.eventbox.entities.Asset;
import com.ht.eventbox.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, String> {

}
