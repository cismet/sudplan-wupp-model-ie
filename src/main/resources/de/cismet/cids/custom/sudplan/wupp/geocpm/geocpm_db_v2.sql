
--SELECT DropGeometryColumn('public', 'geocpm_point', 'geom');
--SELECT DropGeometryColumn('public', 'geocpm_triangle', 'geom');
--SELECT DropGeometryColumn('public', 'geocpm_breaking_edge', 'geom');
--DROP TABLE geocpm_jt_manhole_triangle;
--DROP TABLE geocpm_manhole;
--DROP TABLE geocpm_jt_breaking_edge_triangle;
--DROP TABLE geocpm_breaking_edge;
--DROP TABLE geocpm_curve_value;
--DROP TABLE geocpm_curve;
--DROP TABLE geocpm_source_drain;
--DROP TABLE geocpm_triangle;
--DROP TABLE geocpm_point;
--DROP TABLE geocpm_configuration;

-- we use an integer pkey so that there won't be an issue with cids
-- we don't use serial because of cids, too
CREATE TABLE geocpm_configuration (
    id INTEGER PRIMARY KEY,
    calc_begin TIMESTAMP,
    calc_end TIMESTAMP,
    write_node BOOLEAN,
    write_edge BOOLEAN,
    last_values BOOLEAN,
    save_marked BOOLEAN,
    merge_triangles BOOLEAN,
    min_calc_triangle_size NUMERIC(20,8),
    time_step_restriction BOOLEAN,
    save_velocity_curves BOOLEAN,
    save_flow_curves BOOLEAN,
    result_save_limit NUMERIC(20,8),
    number_of_threads INTEGER,
    q_in INTEGER,
    q_out INTEGER,
    geom INTEGER,

    FOREIGN KEY (geom) REFERENCES geom
);
-- we use the geom table of cids

-- this is for cids
CREATE SEQUENCE geocpm_configuration_seq MINVALUE 1 START 1;
ALTER TABLE geocpm_configuration ALTER COLUMN id SET DEFAULT nextval('geocpm_configuration_seq');

-- configuration proxy to track changes to breaking edges
CREATE SEQUENCE geocpm_configuration_proxy_seq MINVALUE 1 START 1;
CREATE TABLE geocpm_configuration_proxy( 
    id INTEGER PRIMARY KEY DEFAULT nextval('geocpm_configuration_proxy_seq'),
    geocpm_configuration_id INTEGER,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
);


CREATE TABLE geocpm_point (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    index INTEGER,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
);
SELECT AddGeometryColumn('public', 'geocpm_point', 'geom', 31466, 'POINT', 3);

CREATE TABLE geocpm_triangle (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    index INTEGER,
    geocpm_point_a_id BIGINT,
    geocpm_point_b_id BIGINT,
    geocpm_point_c_id BIGINT,
    neighbour_a_id INTEGER,
    neighbour_b_id INTEGER,
    neighbour_c_id INTEGER,
    roughness NUMERIC(18, 3),
    loss NUMERIC(18, 3),
    be_height_a NUMERIC(18, 3),
    be_height_b NUMERIC(18, 3),
    be_height_c NUMERIC(18, 3),
    marked BOOLEAN,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration,
    FOREIGN KEY (geocpm_point_a_id) REFERENCES geocpm_point,
    FOREIGN KEY (geocpm_point_b_id) REFERENCES geocpm_point,
    FOREIGN KEY (geocpm_point_c_id) REFERENCES geocpm_point
);
SELECT AddGeometryColumn('public', 'geocpm_triangle', 'geom', 31466, 'POLYGON', 3);

-- we use an integer pkey so that there won't be an issue with cids
-- we don't use serial because of cids, too
CREATE TABLE geocpm_breaking_edge (
    id INTEGER PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    index INTEGER,
    type INTEGER,
    height numeric(14, 2),
    triangle_count_high INTEGER,
    triangle_count_low INTEGER,
    geom INTEGER,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration,
    FOREIGN KEY (geom) REFERENCES geom
);
-- we use the geom table of cids
--SELECT AddGeometryColumn('public', 'geocpm_breaking_edge', 'geom', 31466, 'LINESTRING', 2);

-- this is for cids
CREATE SEQUENCE geocpm_breaking_edge_seq MINVALUE 1 START 1;
ALTER TABLE geocpm_breaking_edge ALTER COLUMN id SET DEFAULT nextval('geocpm_breaking_edge_seq');

--CREATE SEQUENCE geocpm_breaking_edge_proxy_seq MINVALUE 1 START 1;
---- Creates the new table "geocpm_breaking_edge_proxy" with the columns "height NUMERIC NULL, geocpm_configuration_proxy INTEGER NULL, id INTEGER PRIMARY KEY DEFAULT nextval('geocpm_breaking_edge_proxy_seq')".
--CREATE TABLE geocpm_breaking_edge_proxy(
--    id INTEGER PRIMARY KEY DEFAULT nextval('geocpm_breaking_edge_proxy_seq'),
--    geocpm_configuration_proxy INTEGER, 
--    height NUMERIC(14,2),
--
--    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
--);


CREATE TABLE geocpm_jt_breaking_edge_triangle (
    id BIGSERIAL PRIMARY KEY,
    geocpm_breaking_edge_id INTEGER,
    geocpm_triangle_id BIGINT,
    orientation char(1),

    FOREIGN KEY (geocpm_breaking_edge_id) REFERENCES geocpm_breaking_edge,
    FOREIGN KEY (geocpm_triangle_id) REFERENCES geocpm_triangle
);


CREATE TABLE geocpm_curve (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    identifier VARCHAR(200),

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
);

CREATE TABLE geocpm_curve_value (
    id BIGSERIAL PRIMARY KEY,
    geocpm_curve_id BIGINT,
    t NUMERIC(18, 3),
    value NUMERIC(18, 3),

    FOREIGN KEY (geocpm_curve_id) REFERENCES geocpm_curve
);

CREATE TABLE geocpm_source_drain (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    identifier VARCHAR(200),
    geocpm_triangle_id BIGINT,
    max_capacity NUMERIC(18, 3),
    geocpm_curve_id BIGINT,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration,
    FOREIGN KEY (geocpm_triangle_id) REFERENCES geocpm_triangle,
    FOREIGN KEY (geocpm_curve_id) REFERENCES geocpm_curve
);

CREATE TABLE geocpm_manhole (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    internal_id INTEGER,
--    cap_height NUMERIC(14, 2),
--    entry_profile NUMERIC(14, 2),
--    loss_overfall NUMERIC(14, 2),
--    loss_emersion NUMERIC(14, 2),
--    length_emersion NUMERIC(14, 2),

    cap_height NUMERIC(17, 2),
    entry_profile NUMERIC(17, 2),
    loss_overfall NUMERIC(17, 2),
    loss_emersion NUMERIC(17, 2),
    length_emersion NUMERIC(17, 2),


    name VARCHAR(200),

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
);

CREATE TABLE geocpm_jt_manhole_triangle (
    id BIGSERIAL PRIMARY KEY,
    geocpm_manhole_id BIGINT,
    geocpm_triangle_id BIGINT,

    FOREIGN KEY (geocpm_manhole_id) REFERENCES geocpm_manhole,
    FOREIGN KEY (geocpm_triangle_id) REFERENCES geocpm_triangle
);



------------

CREATE TABLE tmp_bk_triangle_table 
(
--   id BIGSERIAL PRIMARY KEY,
   configuration_id BIGINT, 
   triangle_index BIGINT, 
   breaking_edge_index BIGINT, 
   orientation char(1)
);


CREATE INDEX geocpm_triangle_index_idx ON geocpm_triangle  (index);

--select ST_GeomFromEWKT('SRID=31466;POINT(2575894.570 5679685.710 159.280)')

--select ST_AsEWKT('01010000A0EA7A00008FC2F5480BA74341D7A3706D91AA5541295C8FC2F5E86340')


--CREATE TABLE geocpm_point (
--    id BIGSERIAL PRIMARY KEY,
--    index INTEGER
--);
--SELECT AddGeometryColumn('public', 'geocpm_point', 'geom', 31466, 'POINT', 3);
--
--INSERT INTO geocpm_point (index, geom) VALUES (0, ST_GeomFromEWKT('SRID=31466;POINT(2575894.570 5679685.710 159.280)'));
--INSERT INTO geocpm_point (index, geom) VALUES (1, ST_GeomFromEWKT('SRID=31466;POINT(2575703.110 5680003.560 162.890)'));
--INSERT INTO geocpm_point (index, geom) VALUES (2, ST_GeomFromEWKT('SRID=31466;POINT(2575696.220 5680004.990 162.890)'));
--
--select ST_MakePolygon(ST_MakeLine(array[
--    (SELECT geom FROM geocpm_point WHERE index = 0),
--    (SELECT geom FROM geocpm_point WHERE index = 1),
--    (SELECT geom FROM geocpm_point WHERE index = 2)]));
--
--select ST_MakeLine(geom) from geocpm_point WHERE index = 0 or index = 1 or index = 2;

--ALTER TABLE geocpm_triangle ADD COLUMN tmp_point_a_id INTEGER;
--ALTER TABLE geocpm_triangle ADD COLUMN tmp_point_b_id INTEGER;
--ALTER TABLE geocpm_triangle ADD COLUMN tmp_point_c_id INTEGER;
--
--UPDATE geocpm_triangle gt SET geocpm_point_a_id = gp.id FROM geocpm_point gp WHERE gp.index = gt.tmp_point_a_id
--
--ALTER TABLE geocpm_triangle DROP COLUMN tmp_point_a_id;
--ALTER TABLE geocpm_triangle DROP COLUMN tmp_point_b_id;
--ALTER TABLE geocpm_triangle DROP COLUMN tmp_point_c_id;