-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema global_trade_log_corp
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema global_trade_log_corp
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `global_trade_log_corp` DEFAULT CHARACTER SET utf8 ;
USE `global_trade_log_corp` ;

-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`customers`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`customers` (
  `user_id` INT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(45) NOT NULL,
  `is_active` VARCHAR(45) NOT NULL DEFAULT 'true',
  `full_name` VARCHAR(45) NOT NULL,
  `mobile_1` VARCHAR(45) NULL,
  `mobile_2` VARCHAR(45) NULL,
  `address` VARCHAR(45) NULL,
  `country` VARCHAR(45) NULL,
  PRIMARY KEY (`user_id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`products`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`products` (
  `product_id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  `description` VARCHAR(45) NOT NULL,
  `product_image` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`product_id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`orders`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`orders` (
  `order_id` INT NOT NULL AUTO_INCREMENT,
  `ordered_at` DATETIME NOT NULL,
  `total_price` DOUBLE NOT NULL,
  `customers_customer_id` INT NOT NULL,
  PRIMARY KEY (`order_id`),
  INDEX `fk_orders_users1_idx` (`customers_customer_id` ASC) VISIBLE,
  CONSTRAINT `fk_orders_users1`
    FOREIGN KEY (`customers_customer_id`)
    REFERENCES `global_trade_log_corp`.`customers` (`user_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`order_items`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`order_items` (
  `order_item_id` INT NOT NULL AUTO_INCREMENT,
  `qty` INT NOT NULL,
  `unit_price` DOUBLE NOT NULL,
  `products_product_id` INT NOT NULL,
  `orders_order_id` INT NOT NULL,
  PRIMARY KEY (`order_item_id`),
  INDEX `fk_order_items_products1_idx` (`products_product_id` ASC) VISIBLE,
  INDEX `fk_order_items_orders1_idx` (`orders_order_id` ASC) VISIBLE,
  CONSTRAINT `fk_order_items_products1`
    FOREIGN KEY (`products_product_id`)
    REFERENCES `global_trade_log_corp`.`products` (`product_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_order_items_orders1`
    FOREIGN KEY (`orders_order_id`)
    REFERENCES `global_trade_log_corp`.`orders` (`order_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`suppliers`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`suppliers` (
  `supplier_id` INT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(45) NOT NULL,
  `is_active` VARCHAR(45) NOT NULL DEFAULT 'true',
  `full_name` VARCHAR(45) NOT NULL,
  `mobile_1` VARCHAR(45) NOT NULL,
  `mobile_2` VARCHAR(45) NULL,
  `address` VARCHAR(45) NOT NULL,
  `country` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`supplier_id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`purchase_orders`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`purchase_orders` (
  `po_id` INT NOT NULL AUTO_INCREMENT,
  `suppliers_supplier_id` INT NOT NULL,
  `created_at` DATETIME NOT NULL,
  `total_price` DOUBLE NOT NULL,
  `is_completed` TINYINT NOT NULL,
  `products_product_id` INT NOT NULL,
  `requesting_qty` INT NOT NULL,
  INDEX `fk_table1_suppliers1_idx` (`suppliers_supplier_id` ASC) VISIBLE,
  PRIMARY KEY (`po_id`),
  INDEX `fk_purchase_orders_products1_idx` (`products_product_id` ASC) VISIBLE,
  CONSTRAINT `fk_table1_suppliers1`
    FOREIGN KEY (`suppliers_supplier_id`)
    REFERENCES `global_trade_log_corp`.`suppliers` (`supplier_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_purchase_orders_products1`
    FOREIGN KEY (`products_product_id`)
    REFERENCES `global_trade_log_corp`.`products` (`product_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`grns`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`grns` (
  `grn_id` INT NOT NULL AUTO_INCREMENT,
  `suppliers_supplier_id` INT NOT NULL,
  `created_at` DATETIME NOT NULL,
  `purchase_orders_po_id` INT NOT NULL,
  `products_product_id` INT NOT NULL,
  `qty` INT NOT NULL,
  PRIMARY KEY (`grn_id`),
  INDEX `fk_grns_suppliers1_idx` (`suppliers_supplier_id` ASC) VISIBLE,
  INDEX `fk_grns_purchase_orders1_idx` (`purchase_orders_po_id` ASC) VISIBLE,
  INDEX `fk_grns_products1_idx` (`products_product_id` ASC) VISIBLE,
  CONSTRAINT `fk_grns_suppliers1`
    FOREIGN KEY (`suppliers_supplier_id`)
    REFERENCES `global_trade_log_corp`.`suppliers` (`supplier_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_grns_purchase_orders1`
    FOREIGN KEY (`purchase_orders_po_id`)
    REFERENCES `global_trade_log_corp`.`purchase_orders` (`po_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_grns_products1`
    FOREIGN KEY (`products_product_id`)
    REFERENCES `global_trade_log_corp`.`products` (`product_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`wearhouses`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`wearhouses` (
  `wearhous_id` INT NOT NULL,
  `country` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`wearhous_id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`inventory`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`inventory` (
  `inventory_id` INT NOT NULL,
  `wearhouses_wearhous_id` INT NOT NULL,
  `products_product_id` INT NOT NULL,
  `qty` INT NOT NULL,
  `reorder_level` INT NOT NULL,
  `last_updated_at` DATETIME NOT NULL DEFAULT now(),
  `unit_price` DOUBLE NOT NULL,
  PRIMARY KEY (`inventory_id`),
  INDEX `fk_inventory_wearhouses1_idx` (`wearhouses_wearhous_id` ASC) VISIBLE,
  INDEX `fk_inventory_products1_idx` (`products_product_id` ASC) VISIBLE,
  CONSTRAINT `fk_inventory_wearhouses1`
    FOREIGN KEY (`wearhouses_wearhous_id`)
    REFERENCES `global_trade_log_corp`.`wearhouses` (`wearhous_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_inventory_products1`
    FOREIGN KEY (`products_product_id`)
    REFERENCES `global_trade_log_corp`.`products` (`product_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`audit_records`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`audit_records` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME NOT NULL DEFAULT NOW(),
  `resource` VARCHAR(45) NOT NULL,
  `action` VARCHAR(45) NOT NULL,
  `details` VARCHAR(45) NULL,
  `type` VARCHAR(45) NOT NULL,
  `reference` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`id`, `reference`, `type`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`logs`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`logs` (
  `created_at` INT NOT NULL,
  `idempotency_key` VARCHAR(45) NOT NULL,
  `log_level` VARCHAR(45) NOT NULL,
  `messages` VARCHAR(45) NOT NULL,
  `class_name` VARCHAR(45) NOT NULL,
  `method_name` VARCHAR(45) NOT NULL,
  `file_name` VARCHAR(45) NOT NULL,
  `line_nuber` VARCHAR(45) NOT NULL,
  `thread_name` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`created_at`, `idempotency_key`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`shipments`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`shipments` (
  `shipment_id` INT NOT NULL AUTO_INCREMENT,
  `tracking_number` VARCHAR(45) NOT NULL,
  `vessal_id` VARCHAR(45) NOT NULL,
  `type` VARCHAR(45) NOT NULL,
  `wearhouses_wearhous_id` INT NOT NULL,
  `status` VARCHAR(45) NOT NULL,
  `shipment_type` VARCHAR(45) NULL,
  `ref` VARCHAR(45) NULL,
  PRIMARY KEY (`shipment_id`),
  INDEX `fk_supplier_shipments_wearhouses1_idx` (`wearhouses_wearhous_id` ASC) VISIBLE,
  CONSTRAINT `fk_supplier_shipments_wearhouses1`
    FOREIGN KEY (`wearhouses_wearhous_id`)
    REFERENCES `global_trade_log_corp`.`wearhouses` (`wearhous_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`custom_clearence_records`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`custom_clearence_records` (
  `record_id` INT NOT NULL AUTO_INCREMENT,
  `declaration_number` VARCHAR(45) NULL,
  `supplier_shipments_shipment_id` INT NOT NULL,
  `status` VARCHAR(45) NULL,
  PRIMARY KEY (`record_id`),
  INDEX `fk_custom_clearence_records_supplier_shipments1_idx` (`supplier_shipments_shipment_id` ASC) VISIBLE,
  CONSTRAINT `fk_custom_clearence_records_supplier_shipments1`
    FOREIGN KEY (`supplier_shipments_shipment_id`)
    REFERENCES `global_trade_log_corp`.`shipments` (`shipment_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `global_trade_log_corp`.`supplier_providing_products`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `global_trade_log_corp`.`supplier_providing_products` (
  `products_product_id` INT NOT NULL,
  `suppliers_supplier_id` INT NOT NULL,
  `wearhouses_wearhous_id` INT NOT NULL,
  `lead_time_in_days` INT NOT NULL,
  INDEX `fk_products_has_suppliers_suppliers1_idx` (`suppliers_supplier_id` ASC) VISIBLE,
  INDEX `fk_products_has_suppliers_products1_idx` (`products_product_id` ASC) VISIBLE,
  PRIMARY KEY (`products_product_id`, `suppliers_supplier_id`, `wearhouses_wearhous_id`),
  INDEX `fk_supplier_providing_products_wearhouses1_idx` (`wearhouses_wearhous_id` ASC) VISIBLE,
  CONSTRAINT `fk_products_has_suppliers_products1`
    FOREIGN KEY (`products_product_id`)
    REFERENCES `global_trade_log_corp`.`products` (`product_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_products_has_suppliers_suppliers1`
    FOREIGN KEY (`suppliers_supplier_id`)
    REFERENCES `global_trade_log_corp`.`suppliers` (`supplier_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_supplier_providing_products_wearhouses1`
    FOREIGN KEY (`wearhouses_wearhous_id`)
    REFERENCES `global_trade_log_corp`.`wearhouses` (`wearhous_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
