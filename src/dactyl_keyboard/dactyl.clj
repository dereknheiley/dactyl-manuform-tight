(ns dactyl-keyboard.dactyl
  (:refer-clojure :exclude [use import])
  (:require [clojure.core.matrix :refer [array matrix mmul]]
            [scad-clj.scad :refer :all]
            [scad-clj.model :refer :all]))

(defn deg2rad [degrees]
  (* (/ degrees 180) pi))

(defn debug [shape]
  (color [0.5 0.5 0.5 0.5] shape))

(def WHI [255/255 255/255 255/255 1])
(def RED [255/255 0/255 0/255 1])
(def ORA [220/255 128/255 0/255 1])
(def YEL [220/255 255/255 0/255 1])
(def GRE [0/255 255/255 0/255 1])
(def CYA [0/255 255/255 255/255 1])
(def BLU [0/255 128/255 255/255 1])
(def NBL [0/255 0/255 255/255 1])
(def PUR [127/255 0/255 255/255 1])
(def PIN [255/255 0/255 255/255 1])
(def MAG [255/255 0/255 127/255 1])
(def BRO [102/255 51/255 0/255 1])
(def BLA [0/255 0/255 0/255 1])
(def KEYCAP [220/255 163/255 163/255 1])

;;;;;;;;;;;;;;;;;;;;;;
;; Shape parameters ;;
;;;;;;;;;;;;;;;;;;;;;;

(def nrows 5)
(def ncols 6)

(def adjustable-wrist-rest-holder-plate true)
(def recess-bottom-plate true)

(defn column-curvature [column] 
              (cond  (= column 0)  (deg2rad 20) ;;index outer
                     (= column 1)  (deg2rad 20) ;;index
                     (= column 2)  (deg2rad 17) ;;middle
                     (= column 3)  (deg2rad 17) ;;ring
                     (= column 4)  (deg2rad 22) ;;pinky outer
                     (>= column 5) (deg2rad 22) ;;pinky outer
                     :else 0 ))
(def row-curvature (deg2rad (case nrows 6 1
                                        5 1
                                        4 4)))  ; curvature of the rows
(defn centerrow [column] (case nrows
    6 3.1
    5 (cond  (= column 0)  2.0 ;;index outer
             (= column 1)  2.0 ;;index
             (= column 2)  2.1 ;;middle
             (= column 3)  2.1 ;;ring
             (= column 4)  1.8 ;;pinky outer
             (>= column 5) 1.8 ;;pinky outer
             :else 0 )
    4 1.75))
(def centercol 3)                               ; controls left-right tilt / tenting (higher number is more tenting)
(def tenting-angle (deg2rad (case nrows 6 30
                                        5 35
                                        4 18))) ; or, change this for more precise tenting control
(defn column-offset [column] (cond
                               (= column 0) [0 -5 1]   ;;index outer
                               (= column 1) [0 -5 1]   ;;index
                               (= column 2) [0 3 -5.5] ;;middle
                               (= column 3) [0 0 0]    ;;ring
                               (= column 4) [0 -12 6]  ;;pinky outer
                               (>= column 5) [0 -14 6] ;;pinky outer
                               :else [0 0 0]))

(def keyboard-z-offset (case nrows 
    6 20
    5 22.5 
    4 9))                     ; controls overall height; original=9 with centercol=3; use 16 for centercol=2
(def extra-width 2)           ; extra horizontal space between the base of keys;
(defn extra-height [column]   ; extra vertical space between the base of keys
              (cond  (= column 0)  1.9 ;;index outer
                     (= column 1)  1.9 ;;index
                     (= column 2)  1.7 ;;middle
                     (= column 3)  1.7 ;;ring
                     (= column 4)  2.0 ;;pinky outer
                     (>= column 5) 2.0 ;;pinky outer
                     :else 0 ))

(def wall-z-offset -7)  ; length of the first downward-sloping part of the wall (negative)
(def wall-xy-offset 1)
(def wall-thickness 1)  ; wall thickness parameter

;;;;;;;;;;;;;;;;;;;;;;;
;; General variables ;;
;;;;;;;;;;;;;;;;;;;;;;;

(def lastrow (dec nrows))
(def cornerrow (dec lastrow))
(def lastcol (dec ncols))

;;;;;;;;;;;;;;;;;
;; Switch Hole ;;
;;;;;;;;;;;;;;;;;

(def keyswitch-height 13.8)                                   ;; Was 14.1, then 14.25
(def keyswitch-width 13.9)
(def plate-thickness 5)
(def use_hotswap false)

(def retention-tab-thickness 1.5)
(def retention-tab-hole-thickness (- plate-thickness retention-tab-thickness))
(def mount-width (+ keyswitch-width 3))
(def mount-height (+ keyswitch-height 3))

;for the bottom
(def filled-plate
  (->> (cube mount-height mount-width plate-thickness)
       (translate [0 0 (/ plate-thickness 2)])
       ))

(def holder-x mount-width)
(def holder-thickness    (/ (- holder-x keyswitch-width) 2))
(def holder-y            (+ keyswitch-height (* holder-thickness 2)))
(def swap-z              3)
(def web-thickness (if use_hotswap (+ plate-thickness swap-z) plate-thickness))
(def keyswitch-below-plate (- 8 web-thickness))           ; approx space needed below keyswitch
(def north_facing true)
(def LED-holder true)
(def square-led-size     6)

(def switch-teeth-cutout
  (let [
        ; cherry, gateron, kailh switches all have a pair of tiny "teeth" that stick out
        ; on the top and bottom, this gives those teeth somewhere to press into
        teeth-x        4.5
        teeth-y        0.75
        teeth-z        1.75
        teeth-x-offset 0
        teeth-y-offset (+ (/ keyswitch-height 2) (/ teeth-y 2.01))
        teeth-z-offset (- plate-thickness 1.95)
       ]
      (->> (cube teeth-x teeth-y teeth-z)
           (translate [teeth-x-offset teeth-y-offset teeth-z-offset])
      )
  )
)

(def hotswap-y1          4.3) ;first y-size of kailh hotswap holder
(def hotswap-y2          6.2) ;second y-size of kailh hotswap holder
(def hotswap-z           (+ swap-z 0.5));thickness of kailn hotswap holder + some margin of printing error (0.5mm)
(def hotswap-cutout-z-offset -2.6)
(def hotswap-cutout-1-y-offset 4.95)
(def hotswap-holder
  (let [
        
        ;irregularly shaped hot swap holder
        ; ___________
        ;|_|_______| |  hotswap offset from out edge of holder with room to solder
        ;|_|_O__  \ _|  hotswap pin
        ;|      \O_|_|  hotswap pin
        ;|  o  O  o  |  fully supported friction holes
        ;|    ___    |  
        ;|    |_|    |  space for LED under SMD or transparent switches
        ;
        ; can be described as having two sizes in the y dimension depending on the x coordinate
        
        swap-x              holder-x
        swap-y              (if (or (> 11.5 holder-y) LED-holder) holder-y 11.5) ; should be less than or equal to holder-y
        
        swap-offset-x       0
        swap-offset-y       (/ (- holder-y swap-y) 2)
        swap-offset-z       (* (/ swap-z 2) -1) ; the bottom of the hole. 
        swap-holder         (->> (cube swap-x swap-y swap-z)
                                 (translate [swap-offset-x 
                                             swap-offset-y
                                             swap-offset-z]))
        hotswap-x           holder-x ;cutout full width of holder instead of only 14.5mm
        hotswap-x2          (* (/ holder-x 3) 1.95)
        hotswap-x3          (/ holder-x 4)

        hotswap-cutout-1-x-offset 0.01
        hotswap-cutout-2-x-offset (* (/ holder-x 4.5) -1)
        hotswap-cutout-3-x-offset (- (/ holder-x 2) (/ hotswap-x3 2))
        hotswap-cutout-4-x-offset (- (/ hotswap-x3 2) (/ holder-x 2))
        hotswap-cutout-led-x-offset 0
        hotswap-cutout-1-y-offset 4.95
        hotswap-cutout-2-y-offset 4
        hotswap-cutout-3-y-offset (/ holder-y 2)
        hotswap-cutout-led-y-offset -6
        
        hotswap-cutout-1    (->> (cube hotswap-x hotswap-y1 hotswap-z)
                                 (translate [hotswap-cutout-1-x-offset 
                                             hotswap-cutout-1-y-offset 
                                             hotswap-cutout-z-offset]))
        hotswap-cutout-2    (->> (cube hotswap-x2 hotswap-y2 hotswap-z)
                                 (translate [hotswap-cutout-2-x-offset 
                                             hotswap-cutout-2-y-offset 
                                             hotswap-cutout-z-offset]))
        hotswap-cutout-3    (->> (cube hotswap-x3 hotswap-y1 hotswap-z)
                                 (translate [ hotswap-cutout-3-x-offset
                                              hotswap-cutout-3-y-offset
                                              hotswap-cutout-z-offset]))
        hotswap-cutout-4    (->> (cube hotswap-x3 hotswap-y1 hotswap-z)
                                 (translate [ hotswap-cutout-4-x-offset
                                              hotswap-cutout-3-y-offset
                                              hotswap-cutout-z-offset]))
        hotswap-led-cutout  (->> (cube square-led-size square-led-size 10)
                                 (translate [ hotswap-cutout-led-x-offset
                                              hotswap-cutout-led-y-offset
                                              hotswap-cutout-z-offset]))
        ; for the main axis
        main-axis-hole      (->> (cylinder (/ 4.1 2) 10)
                                 (with-fn 12))
        plus-hole           (->> (cylinder (/ 3.3 2) 10)
                                 (with-fn 8)
                                 (translate [-3.81 2.54 0]))
        minus-hole          (->> (cylinder (/ 3.3 2) 10)
                                 (with-fn 8)
                                 (translate [2.54 5.08 0]))
        friction-hole       (->> (cylinder (/ 1.95 2) 10)
                                 (with-fn 8))
        friction-hole-right (translate [5 0 0] friction-hole)
        friction-hole-left  (translate [-5 0 0] friction-hole)
       ]
      (difference swap-holder
                  main-axis-hole
                  plus-hole
                  minus-hole
                  friction-hole-left
                  friction-hole-right
                  hotswap-cutout-1
                  hotswap-cutout-2
                  hotswap-cutout-3
                  hotswap-cutout-4
                  hotswap-led-cutout)
  )
)

(def switch-dogbone-cutout
  (let [ cutout-radius 1
         cutout (->> (cylinder cutout-radius 10)
                            (with-fn 8))
         cutout-x (- (/ keyswitch-width 2) (/ cutout-radius 2))
         cutout-y (- (/ keyswitch-height 2) (/ cutout-radius 2))
       ]
    (union
      (translate [cutout-x cutout-y 0] cutout)
      (translate [(* -1 cutout-x) cutout-y 0] cutout)
      (translate [cutout-x (* -1 cutout-y) 0] cutout)
    )
  )
)

(defn single-plate [mirror-internals]
  (let [top-wall (->> (cube (+ keyswitch-height 3) 1.5 plate-thickness)
                      (translate [0
                                  (+ (/ 1.5 2) (/ keyswitch-height 2))
                                  (/ plate-thickness 2)]))
        left-wall (->> (cube 1.5 (+ keyswitch-height 3) plate-thickness)
                       (translate [(+ (/ 1.5 2) (/ keyswitch-width 2))
                                   0
                                   (/ plate-thickness 2)]))
        plate-half (difference (union top-wall left-wall) 
                               switch-teeth-cutout
                               switch-dogbone-cutout)
        plate (union plate-half
                  (->> plate-half
                       (mirror [1 0 0])
                       (mirror [0 1 0]))
                  (if use_hotswap 
                      (if north_facing
                          (->> hotswap-holder
                               (mirror [1 0 0])
                               (mirror [0 1 0])
                          )
                          hotswap-holder
                      )
                  )
              )
       ]
    (->> (if mirror-internals
           (->> plate (mirror [1 0 0]))
           plate
         )
    )
  )
)

;amoeba is 16 mm high
(def switch-bottom
  (translate [0 0 (/ keyswitch-below-plate -2)] 
             (cube 16 keyswitch-width keyswitch-below-plate)))

(def hotswap-case-cutout
  (translate [0 (* hotswap-cutout-1-y-offset -1) hotswap-cutout-z-offset] 
             (cube (+ keyswitch-width 3) hotswap-y1 hotswap-z)))

;;;;;;;;;;;;;;;;
;; SA Keycaps ;;
;;;;;;;;;;;;;;;;

(def sa-length 18.25)
(def sa-height 12.5)

(def sa-key-height-from-plate 7.39)
(def sa-cap-bottom-height (+ sa-key-height-from-plate plate-thickness))
(def sa-cap-bottom-height-pressed (+ 3 plate-thickness))

(def sa-double-length 37.5)
(defn sa-cap [keysize]
    (let [ bl2 (case keysize 1   (/ sa-length 2)
                             1.5 (/ sa-length 2)
                             2      sa-length   )
           bw2 (case keysize 1   (/ sa-length 2)
                             1.5 (/ 27.94 2)
                             2   (/ sa-length 2))
           m 8.25
           keycap-xy (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
           keycap-top (case keysize 1    (polygon [[6  6] [  6  -6] [ -6  -6] [-6  6]])
                                    1.52 (polygon [[11 6] [-11   6] [-11  -6] [11 -6]])
                                    2    (polygon [[6 16] [  6 -16] [ -6 -16] [-6 16]]))
           key-cap (hull (->> keycap-xy
                                     (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                     (translate [0 0 0.05]))
                                (->> (polygon [[m m] [m (- m)] [(- m) (- m)] [(- m) m]])
                                     (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                     (translate [0 0 (/ sa-height 2)]))
                                (->> keycap-top
                                     (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                     (translate [0 0 sa-height])))
         ]
         (union 
           (->> key-cap
                (translate [0 0 sa-cap-bottom-height])
                (color KEYCAP))
           (debug (->> key-cap
                (translate [0 0 sa-cap-bottom-height-pressed])))
         )
    )
)

(defn sa-cap-cutout [keysize]
    (let [ cutout-x 0.3
           cutout-y 1.3
           bl2 (case keysize 
                     1   (+ (/ sa-length 2) cutout-y)
                     1.5 (+ (/ sa-length 2) cutout-y)
                     2   (+ sa-length cutout-y))
           bw2 (case keysize
                     1   (+ (/ sa-length 2) cutout-x)
                     1.5 (+ (/ 27.94 2) cutout-x)
                     2   (+ (/ sa-length 2) cutout-x))
           keycap-cutout-xy (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
           key-cap-cutout (hull (->> keycap-cutout-xy
                                     (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                     (translate [0 0 0.05]))
                                (->> keycap-cutout-xy
                                     (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                     (translate [0 0 (* sa-height 1)])))
         ]
         (->> key-cap-cutout
              (translate [0 0 (- sa-cap-bottom-height-pressed 2.99)]))
    )
)

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Placement Functions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(def columns (range 0 ncols))
(def rows (range 0 nrows))

(defn row-radius [column] (+ (/ (/ (+ mount-height (extra-height column)) 2)
                      (Math/sin (/ (column-curvature column) 2)))
                   sa-cap-bottom-height))
(def column-radius (+ (/ (/ (+ mount-width extra-width) 2)
                         (Math/sin (/ row-curvature 2)))
                      sa-cap-bottom-height))
(def column-x-delta (+ -1 (- (* column-radius (Math/sin row-curvature)))))

(defn apply-key-geometry [translate-fn rotate-x-fn rotate-y-fn column row shape]
  (let [column-angle (* row-curvature (- centercol column))
        placed-shape (->> shape
                          (translate-fn [0 0 (- (row-radius column))])
                          (rotate-x-fn (* (column-curvature column) (- (centerrow column) row)))
                          (translate-fn [0 0 (row-radius column)])
                          (translate-fn [0 0 (- column-radius)])
                          (rotate-y-fn column-angle)
                          (translate-fn [0 0 column-radius])
                          (translate-fn (column-offset column)))
        column-z-delta (* column-radius (- 1 (Math/cos column-angle)))]

    (->> placed-shape
         (rotate-y-fn tenting-angle)
         (translate-fn [0 0 keyboard-z-offset]))))

(defn key-place [column row shape]
  (apply-key-geometry translate
                      (fn [angle obj] (rotate angle [1 0 0] obj))
                      (fn [angle obj] (rotate angle [0 1 0] obj))
                      column row shape))

(defn rotate-around-x [angle position]
  (mmul
    [[1 0 0]
     [0 (Math/cos angle) (- (Math/sin angle))]
     [0 (Math/sin angle) (Math/cos angle)]]
    position))

(defn rotate-around-y [angle position]
  (mmul
    [[(Math/cos angle) 0 (Math/sin angle)]
     [0 1 0]
     [(- (Math/sin angle)) 0 (Math/cos angle)]]
    position))


(defn rotate-around-z [angle position]
  (mmul
    [[(Math/cos angle) (- (Math/sin angle)) 0]
     [(Math/sin angle) (Math/cos angle) 0]
     [0 0 1]]
    position))

(defn key-position [column row position]
  (apply-key-geometry (partial map +) rotate-around-x rotate-around-y column row position))

(defn key-places [shape]
  (apply union
         (for [column columns
               row rows
               :when (or (.contains [2 3] column)
                         (not= row lastrow))]
           (->> shape
                (key-place column row)))))
(defn key-holes [mirror-internals]
  (key-places (single-plate mirror-internals)))
(def key-fills
  (key-places filled-plate))
(def key-space-below
  (key-places switch-bottom))
(def caps
  (key-places (sa-cap 1)))
(def caps-cutout
  (key-places (sa-cap-cutout 1)))

;;;;;;;;;;;;;;;;;;;;
;; Web Connectors ;;
;;;;;;;;;;;;;;;;;;;;

; posts are located at the inside corners of the key plates.
; the 'web' is the fill between key plates.
;

(def post-size 0.1)
(def web-post (->> (cube post-size post-size web-thickness)
                   (translate [0 0 (+ (/ web-thickness -2)
                                      plate-thickness)])))

(def post-adj (/ post-size 2))
(def web-post-tr (translate [(- (/ mount-width  2) post-adj) (- (/ mount-height  2) post-adj) 0] web-post))
(def web-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height  2) post-adj) 0] web-post))
(def web-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))
(def web-post-br (translate [(- (/ mount-width  2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))


; plate posts for connecting columns together without wasting material
; or blocking sides of hotswap sockets
(def plate-post-thickness (- web-thickness 2))
(def plate-post (->> (cube post-size post-size plate-post-thickness)
                   (translate [0 0 (+ plate-post-thickness (/ plate-post-thickness -1.5)
                                      )])))
(def plate-post-tr (translate [(- (/ mount-width  2) post-adj) (- (/ mount-height  2) post-adj) 0] plate-post))
(def plate-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height  2) post-adj) 0] plate-post))
(def plate-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -2) post-adj) 0] plate-post))
(def plate-post-br (translate [(- (/ mount-width  2) post-adj) (+ (/ mount-height -2) post-adj) 0] plate-post))

; fat web post for very steep angles between thumb and finger clusters
; this ensures the walls stay somewhat thicker
(def fat-post-size 1.2)
(def fat-web-post (->> (cube fat-post-size fat-post-size web-thickness)
                       (translate [0 0 (+ (/ web-thickness -2)
                                          plate-thickness)])))

(def fat-post-adj (/ fat-post-size 2))
(def fat-web-post-tr (translate [(- (/ mount-width  2) fat-post-adj) (- (/ mount-height  2) fat-post-adj) 0] fat-web-post))
(def fat-web-post-tl (translate [(+ (/ mount-width -2) fat-post-adj) (- (/ mount-height  2) fat-post-adj) 0] fat-web-post))
(def fat-web-post-bl (translate [(+ (/ mount-width -2) fat-post-adj) (+ (/ mount-height -2) fat-post-adj) 0] fat-web-post))
(def fat-web-post-br (translate [(- (/ mount-width  2) fat-post-adj) (+ (/ mount-height -2) fat-post-adj) 0] fat-web-post))
; wide posts for 1.5u keys in the main cluster

(defn triangle-hulls [& shapes]
  (apply union
         (map (partial apply hull)
              (partition 3 1 shapes))))

(defn piramid-hulls [top & shapes]
  (apply union
         (map (partial apply hull top)
              (partition 2 1 shapes))))

(def connectors
  (apply union
         (concat
           ;; Row connections
           (for [column (range 0 (dec ncols))
                 row (range 0 lastrow)]
             (if use_hotswap
               (triangle-hulls
                 (key-place (inc column) row plate-post-tl)
                 (key-place      column  row plate-post-tr)
                 (key-place (inc column) row plate-post-bl)
                 (key-place      column  row plate-post-br))
               (triangle-hulls
                 (key-place (inc column) row web-post-tl)
                 (key-place      column  row web-post-tr)
                 (key-place (inc column) row web-post-bl)
                 (key-place      column  row web-post-br))
              ) 
           )

           ;; Column connections
           (for [column columns
                 row (range 0 cornerrow)]
             (triangle-hulls
               (key-place column row web-post-bl)
               (key-place column row web-post-br)
               (key-place column (inc row) web-post-tl)
               (key-place column (inc row) web-post-tr)))

           ;; Diagonal connections
           (for [column (range 0 (dec ncols))
                 row (range 0 cornerrow)]
             (if use_hotswap
               (triangle-hulls
                 (key-place      column       row  plate-post-br)
                 (key-place      column  (inc row) plate-post-tr)
                 (key-place (inc column)      row  plate-post-bl)
                 (key-place (inc column) (inc row) plate-post-tl))
               (triangle-hulls
                 (key-place      column       row  web-post-br)
                 (key-place      column  (inc row) web-post-tr)
                 (key-place (inc column)      row  web-post-bl)
                 (key-place (inc column) (inc row) web-post-tl))
             )
           ))))

;;;;;;;;;;;;
;; Thumbs ;;
;;;;;;;;;;;;

(def thumb-pos (if (> nrows 4) [5.5 1 9] [9 -5 4]))
(def thumb-rot [0 10 0] )

(def thumborigin
  (map + (key-position 1 cornerrow [(/ mount-width 2) (- (/ mount-height 2)) 0])
       thumb-pos))

"need to account for plate thickness which is baked into thumb-_-place rotation & move values
plate-thickness was 2
need to adjust for difference for thumb-z only"
(def thumb-design-z 2)
(def thumb-z-adjustment (- (if (> plate-thickness thumb-design-z)
                                 (- thumb-design-z plate-thickness)
                                 (if (< plate-thickness thumb-design-z)
                                       (- thumb-design-z plate-thickness) 
                                       0)) 
                            1.1))
(def thumb-x-rotation-adjustment (if (> nrows 4) -12 -8))
(defn thumb-place [rot move shape]
  (->> 
    (->> shape
       (translate [0 0 thumb-z-adjustment])                   ;adapt thumb positions for increased plate
       (rotate (deg2rad thumb-x-rotation-adjustment) [1 0 0]) ;adjust angle of all thumbs to be less angled down towards user since key is taller
       
       (rotate (deg2rad (nth rot 0)) [1 0 0])
       (rotate (deg2rad (nth rot 1)) [0 1 0])
       (rotate (deg2rad (nth rot 2)) [0 0 1])
       (translate thumborigin)
       (translate move))

     (rotate (deg2rad (nth thumb-rot 0)) [1 0 0])
     (rotate (deg2rad (nth thumb-rot 1)) [0 1 0])
     (rotate (deg2rad (nth thumb-rot 2)) [0 0 1])))

; convexer
(defn thumb-r-place [shape] (thumb-place [14 -35 10] [-14 -10 5] shape)) ; right
(defn thumb-m-place [shape] (thumb-place [10 -23 20] [-33 -15 -6] shape)) ; middle
(defn thumb-l-place [shape] (thumb-place [6 -5 25] [-53 -23.5 -11.5] shape)) ; left

(defn thumb-layout [shape]
  (union
    (thumb-r-place shape)
    (thumb-m-place shape)
    (thumb-l-place shape)))

(def thumbcaps (thumb-layout (sa-cap 1)))
(def thumbcaps-cutout (thumb-layout (sa-cap-cutout 1)))
(defn thumb [mirror-internals] (thumb-layout (single-plate mirror-internals)))
(def thumb-space-below (thumb-layout switch-bottom))
(def thumb-space-hotswap (thumb-layout hotswap-case-cutout))
;;;;;;;;;;
;; Case ;;
;;;;;;;;;;

(defn bottom [height p]
  (->> (project p)
       (extrude-linear {:height height :twist 0 :convexity 0})
       (translate [0 0 (- (/ height 2) 10)])))

(defn bottom-hull [& p]
  (hull p (bottom 0.001 p)))

(defn wall-locate1 [dx dy] [(* dx wall-thickness)                    (* dy wall-thickness)                    0])
(defn wall-locate2 [dx dy] [(* dx wall-xy-offset)                    (* dy wall-xy-offset)                    wall-z-offset])
(defn wall-locate3 [dx dy] [(* dx (+ wall-xy-offset wall-thickness)) (* dy (+ wall-xy-offset wall-thickness)) wall-z-offset])

(def thumb-connectors
  (union
    (->> (triangle-hulls                                         ; top two
      (thumb-m-place plate-post-tr)
      (thumb-m-place plate-post-br)
      (thumb-r-place plate-post-tl)
      (thumb-r-place plate-post-bl)) (color RED))
    (->> (triangle-hulls                                         ; top two
      (thumb-m-place plate-post-tl)
      (thumb-l-place plate-post-tr)
      (thumb-m-place plate-post-bl)
      (thumb-l-place plate-post-br)
      (thumb-m-place plate-post-bl)) (color ORA))
    (->> (triangle-hulls                                         ; top two to the main keyboard, starting on the left
      (key-place 2 lastrow web-post-br)
      (key-place 3 lastrow web-post-bl)
      (key-place 2 lastrow web-post-tr)
      (key-place 3 lastrow web-post-tl)
      (key-place 3 cornerrow web-post-bl)
      (key-place 3 lastrow web-post-tr)
      (key-place 3 cornerrow web-post-br)
      (key-place 4 cornerrow web-post-bl)
      ) (color BLA))
    (->> (triangle-hulls
      (key-place 1 cornerrow web-post-br)
      (key-place 2 lastrow web-post-tl)
      (key-place 2 cornerrow web-post-bl)
      (key-place 2 lastrow web-post-tr)
      (key-place 2 cornerrow web-post-br)
      (key-place 3 cornerrow web-post-bl)
      ) (color GRE))
    (->> (triangle-hulls
      (key-place 3 lastrow web-post-tr)
      (key-place 3 lastrow web-post-br)
      (key-place 3 lastrow web-post-tr)
      (key-place 4 cornerrow web-post-bl)) (color CYA))
    (hull                                                   ; between thumb m and top key
      (key-place 0 cornerrow (translate (wall-locate1 -1 0) web-post-bl))
      (thumb-m-place web-post-tr)
      (thumb-m-place web-post-tl))
    (piramid-hulls                                          ; top ridge thumb side
      (key-place 0 cornerrow (translate (wall-locate1 -1 0) fat-web-post-bl))
      (key-place 0 cornerrow (translate (wall-locate2 -1 0) web-post-bl))
      (key-place 0 cornerrow web-post-bl)
      ;(thumb-r-place web-post-tr)
      (thumb-r-place web-post-tl)
      (thumb-m-place web-post-tr)
      (key-place 0 cornerrow (translate (wall-locate2 -1 0) web-post-bl))
      )
    (->> (triangle-hulls
      (key-place 0 cornerrow fat-web-post-br)
      (key-place 0 cornerrow fat-web-post-bl)
      (thumb-r-place web-post-tl)
      (key-place 1 cornerrow web-post-bl)
      (key-place 1 cornerrow web-post-br)) (color BLU))
    (->> (triangle-hulls
      (thumb-r-place fat-web-post-tl)
      (thumb-r-place fat-web-post-tr)
      (key-place 1 cornerrow web-post-br)
      ; (key-place 2 lastrow web-post-tl)
      ) (color NBL))
    (->> (triangle-hulls
      (key-place 2 lastrow web-post-tl)
      ; (thumb-r-place fat-web-post-tr)
      ; (key-place 2 lastrow web-post-bl)
      (thumb-r-place fat-web-post-br)) (color PUR))
    (->> (triangle-hulls
      (thumb-r-place web-post-br)
      (key-place 2 lastrow web-post-bl)
      (key-place 3 lastrow web-post-bl)
      (key-place 2 lastrow web-post-br)) (color PIN))
    ))

; dx1, dy1, dx2, dy2 = direction of the wall. '1' for front, '-1' for back, '0' for 'not in this direction'.
; place1, place2 = function that places an object at a location, typically refers to the center of a key position.
; post1, post2 = the shape that should be rendered
(defn wall-brace [place1 dx1 dy1 post1 place2 dx2 dy2 post2]
  (union
    (->> (hull
      (place1 post1)
      (place1 (translate (wall-locate1 dx1 dy1) post1))
      ; (place1 (translate (wall-locate2 dx1 dy1) post1))
      (place1 (translate (wall-locate2 dx1 dy1) post1))
      (place2 post2)
      (place2 (translate (wall-locate1 dx2 dy2) post2))
      ; (place2 (translate (wall-locate2 dx2 dy2) post2))
      (place2 (translate (wall-locate2 dx2 dy2) post2))
      )
    (color BRO))
    (->> (bottom-hull
      (place1 (translate (wall-locate2 dx1 dy1) post1))
      ; (place1 (translate (wall-locate2 dx1 dy1) post1))
      (place2 (translate (wall-locate2 dx2 dy2) post2))
      ; (place2 (translate (wall-locate2 dx2 dy2) post2))
      )
     (color ORA))
  ))

(defn wall-brace-back [place1 dx1 dy1 post1 place2 dx2 dy2 post2]
    (->> (bottom-hull
      (place1 (translate (wall-locate2 dx1 dy1) post1))
      ; (place1 (translate (wall-locate2 dx1 dy1) post1))
      ; (place2 (translate (wall-locate2 dx2 dy2) post2))
      (place2 (translate (wall-locate2 dx2 dy2) post2))
     )
     (color PUR))
)

(defn key-wall-brace [x1 y1 dx1 dy1 post1 x2 y2 dx2 dy2 post2]
  (wall-brace (partial key-place x1 y1) dx1 dy1 post1
              (partial key-place x2 y2) dx2 dy2 post2))

(defn key-wall-brace-back [x1 y1 dx1 dy1 post1 x2 y2 dx2 dy2 post2]
  (wall-brace-back 
              (partial key-place x1 y1) dx1 dy1 post1
              (partial key-place x2 y2) dx2 dy2 post2)
  )

(defn key-corner [x y loc]
  (case loc
    :tl (key-wall-brace x y 0  1 web-post-tl x y -1 0 web-post-tl)
    :tr (key-wall-brace x y 0  1 web-post-tr x y  1 0 web-post-tr)
    :bl (key-wall-brace x y 0 -1 web-post-bl x y -1 0 web-post-bl)
    :br (key-wall-brace x y 0 -1 web-post-br x y  1 0 web-post-br)))

(def right-wall
  (union (key-corner lastcol 0 :tr)
         (for [y (range 0 lastrow)] (key-wall-brace lastcol      y  1 0 web-post-tr lastcol y 1 0 web-post-br))
         (for [y (range 1 lastrow)] (key-wall-brace lastcol (dec y) 1 0 web-post-br lastcol y 1 0 web-post-tr))
         (key-corner lastcol cornerrow :br)))


(def case-walls
  (union
    right-wall
    ; back wall
    (for [x (range 0 ncols)] (key-wall-brace x 0 0 1 web-post-tl      x  0 0 1 web-post-tr))
    (for [x (range 1 ncols)]
      (case x
        2 (key-wall-brace x 0 0 1 fat-web-post-tl (dec x) 0 0 1 fat-web-post-tr)
        3 (key-wall-brace x 0 0 1 fat-web-post-tl (dec x) 0 0 1 fat-web-post-tr)
        4 (key-wall-brace x 0 0 1 fat-web-post-tl (dec x) 0 0 1 fat-web-post-tr)
          (key-wall-brace      x 0 0 1 web-post-tl (dec x) 0 0 1 web-post-tr)
      )
    )
    ; left wall
    (->> (key-wall-brace 0 0 -1 0 web-post-tl 0 0 -1 0 web-post-bl) (color BLA))
    (for [y (range 0 lastrow)] (key-wall-brace 0 y -1 0 web-post-tl 0 y -1 0 web-post-bl))
    (for [y (range 1 lastrow)] (key-wall-brace 0 (dec y) -1 0 web-post-bl 0 y -1 0 web-post-tl))
    (->> (wall-brace (partial key-place 0 cornerrow) -1 0 web-post-bl thumb-m-place 0 1 web-post-tl) (color WHI))

    ; left-back-corner
    (key-wall-brace 0 0 0 1 web-post-tl 0 0 -1 0 web-post-tl)
    ; front wall
    (key-wall-brace 3 lastrow 0   -1 web-post-bl 3   lastrow 0.5 -1 web-post-br)
    (key-wall-brace 3 lastrow 0.5 -1 fat-web-post-br 4 cornerrow 0.5 -1 fat-web-post-bl)
    (for [x (range 4 ncols)] (key-wall-brace x cornerrow 0 -1 fat-web-post-bl      x  cornerrow 0 -1 fat-web-post-br)) ; TODO fix extra wall
    (for [x (range 5 ncols)] (key-wall-brace x cornerrow 0 -1 fat-web-post-bl (dec x) cornerrow 0 -1 fat-web-post-br))
    (->> (wall-brace thumb-r-place 0 -1 fat-web-post-br (partial key-place 3 lastrow) 0 -1 web-post-bl) (color RED))
    ; thumb walls
    (->> (wall-brace thumb-r-place  0 -1 fat-web-post-br thumb-r-place  0 -1 fat-web-post-bl) (color ORA))
    (->> (wall-brace thumb-m-place  0 -1 fat-web-post-br thumb-m-place  0 -1 fat-web-post-bl) (color YEL))
    (->> (wall-brace thumb-l-place  0 -1 fat-web-post-br thumb-l-place  0 -1 fat-web-post-bl) (color GRE))
    (->> (wall-brace thumb-l-place  0  1 fat-web-post-tr thumb-l-place  0  1 fat-web-post-tl) (color CYA))
    (->> (wall-brace thumb-l-place -1  0 fat-web-post-tl thumb-l-place -1  0 fat-web-post-bl) (color BLU))
    ; thumb corners
    (->> (wall-brace thumb-l-place -1  0 fat-web-post-bl thumb-l-place  0 -1 fat-web-post-bl) (color NBL))
    (->> (wall-brace thumb-l-place -1  0 fat-web-post-tl thumb-l-place  0  1 fat-web-post-tl) (color PUR))
    ; thumb tweeners
    (->> (wall-brace thumb-r-place  0 -1 fat-web-post-bl thumb-m-place  0 -1 fat-web-post-br) (color PIN))
    (->> (wall-brace thumb-m-place  0 -1 fat-web-post-bl thumb-l-place  0 -1 fat-web-post-br) (color MAG))
    (->> (wall-brace thumb-m-place  0  1 fat-web-post-tl thumb-l-place  0  1 fat-web-post-tr) (color BRO))
    (->> (wall-brace thumb-l-place -1  0 fat-web-post-bl thumb-l-place -1  0 fat-web-post-tl) (color BLA))
    ))


; Screw insert definition & position
(defn screw-insert-shape [bottom-radius top-radius height]
  (->> (binding [*fn* 30]
         (cylinder [bottom-radius top-radius] height)))
  )

(defn screw-insert [column row bottom-radius top-radius height offset]
  (let [position (key-position column row [0 0 0])]
    (->> (screw-insert-shape bottom-radius top-radius height)
         (translate (map + offset [(first position) (second position) (/ height 2)])))))


(def screw-insert-bottom-offset 0)
(def screw-insert-bc   (if (> nrows 4) [3 3.5 screw-insert-bottom-offset] [-3.2 7 screw-insert-bottom-offset]))
(def screw-insert-ml   (if (> nrows 4) [-6 -13 screw-insert-bottom-offset] [-7 -11 screw-insert-bottom-offset]))
(def screw-insert-thmb (if (> nrows 4) [-24 -15 screw-insert-bottom-offset] [-6 -4.4 screw-insert-bottom-offset]))
(def screw-insert-tr   (if (> nrows 4) [15 1.5 screw-insert-bottom-offset] [23.1 5 screw-insert-bottom-offset]))
(def screw-insert-fc   (if (> nrows 4) [17.5 9.5 screw-insert-bottom-offset] [19 11 screw-insert-bottom-offset]))
(defn screw-insert-all-shapes [bottom-radius top-radius height]
  (union (->> (screw-insert 2 0 bottom-radius top-radius height screw-insert-bc) (color RED)) ; top middle
         (->> (screw-insert 0 1 bottom-radius top-radius height screw-insert-ml) (color PIN)) ; left
         (->> (screw-insert 0 lastrow bottom-radius top-radius height screw-insert-thmb) (color BRO)) ;thumb
         (->> (screw-insert (- lastcol 1) 0 bottom-radius top-radius height screw-insert-tr) (color PUR)) ; top right
         (->> (screw-insert 2 (+ lastrow 1) bottom-radius top-radius height screw-insert-fc) (color BLA)) ))  ;bottom middle

; Hole Depth Y: 4.4
(def screw-insert-height 5.5)

; Hole Diameter C: 4.1-4.4
(def screw-insert-radius (/ 4.4 2))
(def screw-insert-holes ( screw-insert-all-shapes 
                          screw-insert-radius 
                          screw-insert-radius 
                          (* screw-insert-height 1.5)
                        ))

(def screw-insert-wall-thickness 3.5)
(def screw-insert-outers ( screw-insert-all-shapes 
                           (+ screw-insert-radius screw-insert-wall-thickness) 
                           (+ screw-insert-radius screw-insert-wall-thickness) 
                           screw-insert-height
                         ))

(def okke-right (import "../things/okke_right.stl"))
(def okke-right-offset-coordinates (if (> nrows 4) [4 -17.5 2.8] 
                                                   [4.7 0.3 4.2]))
(def okke-right (if (> nrows 4) (->>  okke-right (translate okke-right-offset-coordinates)
                                                 (rotate (deg2rad -12) [1 0 0])
                                                 (rotate (deg2rad 2.75) [0 0 1]) )
                                (translate okke-right-offset-coordinates okke-right))
)

(def usb-holder 
                (mirror [-1 0 0]
                    (import "../things/usb_holder_w_reset.stl")
                )
)
(def usb-holder-cutout-height 30.3)
(def usb-holder-clearance 0.05)
(def usb-holder-bottom-offset 0.05)

(def usb-holder-offset-coordinates (case nrows 
    6 [-24.9 (if use_hotswap 67.3 70.9) usb-holder-bottom-offset]
    5 [-28 (if use_hotswap 54.17 49.5) usb-holder-bottom-offset]
    4 [-41.5 (if use_hotswap 50.28 48.9) usb-holder-bottom-offset]))
(def usb-holder (translate usb-holder-offset-coordinates usb-holder))
(def usb-holder-space
  (translate [0 0 (/ usb-holder-bottom-offset 2)]
  (extrude-linear {:height usb-holder-cutout-height :twist 0 :convexity 0}
                  (offset usb-holder-clearance
                          (project usb-holder))))
  )

(def bottom-plate-thickness 3)
(def screw-insert-fillets-z 2)

(def screw-insert-bottom-plate-bottom-radius (+ screw-insert-radius 0.9))
(def screw-insert-bottom-plate-top-radius    (- screw-insert-radius    0.3))
(def screw-insert-holes-bottom-plate ( screw-insert-all-shapes 
                                       screw-insert-bottom-plate-top-radius 
                                       screw-insert-bottom-plate-top-radius 
                                       (+ bottom-plate-thickness 0.1)
                                     ))

(def screw-insert-fillets-bottom-plate ( screw-insert-all-shapes 
                                         screw-insert-bottom-plate-bottom-radius 
                                         screw-insert-bottom-plate-top-radius 
                                         screw-insert-fillets-z
                                       ))


(defn screw-insert-wrist-rest [bottom-radius top-radius height]
    (for [x (range 0 9)
          y (range 0 9)]
        (translate [(* x 5) (* y 5) 0]
          (screw-insert-shape 
            bottom-radius 
            top-radius 
            height)
        )
    )
)

(defn screw-insert-wrist-rest-four [bottom-radius top-radius height]
    (for [x (range 0 2)
          y (range 0 2)]
        (translate [(* x 20) (* y 20) 0]
          (screw-insert-shape 
            bottom-radius 
            top-radius 
            height)
        )
    )
)

(def wrist-shape-connector (polygon [[35 20] [30 -20] [-30 -20] [-35 20]]))
(def wrist-shape 
    (union 
        (translate [0 -45 0] (cube 60 55 bottom-plate-thickness))
        (translate [0 0 (- (/ bottom-plate-thickness -2) 0.05)]
                   (hull (->> wrist-shape-connector
                              (extrude-linear {:height 0.1 :twist 0 :convexity 0}))
                         (->> wrist-shape-connector
                              (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                              (translate [0 0 bottom-plate-thickness])) ))
    )
)

(def wrist-rest-right
    (import "../things/wrist-rest-right.stl"))
(def wrist-rest-right-holes
    (if adjustable-wrist-rest-holder-plate
        (difference wrist-rest-right
                    (translate [-10 -5 0] 
                               (screw-insert-wrist-rest-four screw-insert-radius
                                                             screw-insert-radius
                                                             99))
                    (translate [-11 39 (- (/ bottom-plate-thickness 2) 0.1)] wrist-shape)
                    (translate [ 11 39 (- (/ bottom-plate-thickness 2) 0.1)] wrist-shape)
        )
        wrist-rest-right
    )
)

(def case-walls-bottom (cut 
                           (translate [0 0 10] 
                                      case-walls
                           )
                       ))
(def case-walls-bottom-projection (project
                                    (union
                                     (extrude-linear {:height 0.01
                                                      :scale 0.997
                                                      :center true} 
                                         case-walls-bottom
                                     )
                                     (extrude-linear {:height 0.01
                                                      :scale 1.11
                                                      :center true} 
                                         case-walls-bottom
                                     )
                                    ) 
                                  ))
(def bottom-plate
  (let [screw-cutouts         (translate [0 0 (/ bottom-plate-thickness -1.99)] 
                                         screw-insert-holes-bottom-plate)
        screw-cutouts-fillets (translate [0 0 (/ bottom-plate-thickness -1.99)] 
                                         screw-insert-fillets-bottom-plate)
        wrist-rest-adjust-fillets (translate [-12 -120 0] 
                                         (screw-insert-wrist-rest screw-insert-bottom-plate-bottom-radius
                                                                  screw-insert-bottom-plate-top-radius
                                                                  screw-insert-fillets-z))
        wrist-rest-adjust-holes (translate [-12 -120 0] 
                                         (screw-insert-wrist-rest screw-insert-bottom-plate-top-radius
                                                                  screw-insert-bottom-plate-top-radius
                                                                  (+ bottom-plate-thickness 0.1)))
        bottom-plate-blank (extrude-linear {:height bottom-plate-thickness}
                               (union
                                   (difference
                                       (project 
                                          (extrude-linear {:height 0.01
                                                           :scale  0
                                                           :center true} 
                                              case-walls-bottom
                                          )
                                       )
                                       (if recess-bottom-plate
                                           case-walls-bottom-projection
                                       )
                                   )
                                   (project
                                       (if adjustable-wrist-rest-holder-plate 
                                                  (translate [8 -55 0] wrist-shape))
                                   )
                                   (project
                                       (if recess-bottom-plate
                                               usb-holder
                                       )
                                   )
                               )
                           )
       ]
    (difference (union 
                       bottom-plate-blank
                       ; (translate [8 -100 0] wrist-rest-right-holes)
                )
                screw-cutouts
                screw-cutouts-fillets
                (if adjustable-wrist-rest-holder-plate
                    (union (translate [0 0 (* -1.01 (/ screw-insert-fillets-z 4))] 
                                      wrist-rest-adjust-fillets)
                           wrist-rest-adjust-holes))
    )
  )
)
(spit "things/single-plate.scad"
      (write-scad (single-plate false)))

(defn model-right [mirror-internals]
  (difference
    (union
      (key-holes mirror-internals)
      connectors
      (thumb mirror-internals)
      thumb-connectors
      (difference (union case-walls
                         screw-insert-outers
                         )
                  usb-holder-space
                  screw-insert-holes
                  ))
    
    (if recess-bottom-plate
        (union
            (translate [0 0 (* -1 (+ 20 bottom-plate-thickness))] (cube 350 350 40))
            (translate [0 0 (* -1 (/ bottom-plate-thickness 2))] bottom-plate)
        )
        (translate [0 0 -20] (cube 350 350 40))
    )
    
    thumb-space-hotswap
    caps-cutout
    thumbcaps-cutout
    key-space-below
    ))
(spit "things/right.scad"
      (write-scad (model-right false)))

(def model-left
  (mirror [-1 0 0] (model-right true))
)
(spit "things/left.scad"
      (write-scad model-left))

(spit "things/right-plate.scad"
      (write-scad bottom-plate))

(spit "things/wrist-rest-right-holes.scad"
      (write-scad wrist-rest-right-holes))

(spit "things/test.scad"
      (write-scad
        (difference
          (union
            (->> (model-right false)
              ; (color BLU)
            )
            ; caps
            ; (debug caps-cutout)
            ; thumbcaps
            ; (debug thumbcaps-cutout)
            (debug key-space-below)
            (debug thumb-space-below)
            (debug thumb-space-hotswap)

            (debug usb-holder)
            ;(debug okke-right)
            (translate [0 0 (* -1 (/ bottom-plate-thickness 2))]
                (debug bottom-plate)
                (translate [8 -100 0] wrist-rest-right-holes)

            )
            )
        )))